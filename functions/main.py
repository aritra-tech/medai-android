import json
import os
from typing import Any

import requests
from firebase_functions import https_fn, options
from firebase_functions.options import set_global_options


set_global_options(region="asia-south1", max_instances=10)

DEFAULT_IMAGE_MIME_TYPE = "image/jpeg"
PRESCRIPTION_MODEL = "gemini-2.5-flash"
MEDICAL_REPORT_MODEL = "gemini-2.5-flash"
GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models"

INVALID_PRESCRIPTION_MESSAGE = (
    "This image does not appear to be a valid medical prescription. "
    "Please upload a clear image of a doctor's prescription."
)
INVALID_MEDICAL_REPORT_MESSAGE = (
    "This image does not appear to be a valid medical report. "
    "Please upload a clear image of a report or test result."
)

PRESCRIPTION_VALIDATION_PROMPT = """
Analyze this image to determine if it contains a valid medical prescription from a doctor or healthcare provider.

Look for these key indicators of a prescription:
1. Doctor's name, signature, or medical license number
2. Patient information
3. Medication names with proper dosages
4. Date of prescription
5. Pharmacy or clinic letterhead/stamp
6. Medical terminology and format
7. Rx symbol or prescription format

Respond with ONLY valid JSON:
{
  "valid": true
}

Return {"valid": false} if the image contains:
- Random text or documents
- Food items or general photos
- Screenshots of non-medical content
- Handwritten notes that aren't prescriptions
- Medicine boxes/bottles
- Generic medical information or articles
""".strip()

PRESCRIPTION_SUMMARY_PROMPT = """
Analyze this prescription image and extract the following information.
Your task is to carefully analyze the content and return a detailed, structured, and patient-friendly response.
Please respond ONLY with valid JSON in exactly this format (no additional text or markdown):

{
  "doctorName": "Dr. [Name] (extract the doctor's full name from the prescription, if not clearly visible use 'Unknown Doctor')",
  "patientInfo": {
    "name": "Full name of the patient",
    "age": "Age with units (e.g., 22 years)",
    "sex": "Male / Female / Other",
    "weight": "Weight with units (e.g., 58 kg)",
    "bloodPressure": "BP in format (systolic/diastolic)",
    "pulse": "Pulse rate with units (e.g., 87 bpm)",
    "oxygenSaturation": "SpO2 percentage (e.g., 98%)",
    "date": "Date of prescription (e.g., 18/01/2025)"
  },
  "diagnosis": {
    "presentingComplaints": "Short description of the problem",
    "provisionalDiagnosis": "Initial diagnosis or impression by the doctor",
    "comorbidities": ["List any comorbid conditions mentioned"],
    "additionalNotes": ["Any other relevant observations or medical history"]
  },
  "medications": [
    {
      "name": "Medication name (validated to be correct)",
      "dosage": "Strength or amount per dose (e.g., 1 tablet, 500mg)",
      "frequency": "How often to take (e.g., twice daily, every 8 hours)",
      "duration": "How long to take it (e.g., 7 days)",
      "route": "Route of administration (e.g., oral, topical)"
    }
  ],
  "instructions": [
    "List of clear patient-friendly instructions based on the prescription"
  ],
  "prescriptionReason": "The main reason or medical condition for which this prescription was issued.",
  "dosageInstructions": [
    "Instructions related to how to take the medicine"
  ],
  "warnings": [
    "Any important warnings, precautions, or side effects mentioned or inferred based on the medicines"
  ],
  "summary": "Summarize the entire prescription in plain, easy-to-understand English.",
  "stepsToCure": [
    "Step-by-step patient-friendly actionable steps needed to recover/cure"
  ]
}

If you cannot clearly read certain information, use "Not clearly visible" for that field.
For doctorName, look for signatures, printed names, letterheads, or any doctor identification.
If found, format as "Dr. [Full Name]". If not clear, use "Unknown Doctor".
Ensure the medicine names exist and are valid.
Avoid medical jargon in the summary; use layman's terms.
Ensure all JSON keys are present even if the arrays are empty.
""".strip()

MEDICAL_REPORT_VALIDATION_PROMPT = """
Analyze this image to determine if it contains a valid medical report, diagnostic test result, lab report, radiology report, or discharge summary.
Look for key indicators like: patient info, doctor or facility details, test names and results, impressions/diagnosis, dates, and structured report formatting.

Respond with ONLY valid JSON:
{
  "valid": true
}

Return {"valid": false} if the image is not clearly a medical report/result document.
""".strip()

MEDICAL_REPORT_SUMMARY_PROMPT = """
You are a medical report assistant. Analyze the attached medical report image and produce a precise, patient-friendly summary.

Return ONLY valid JSON (no preface, no markdown fences) with these keys:
{
  "doctorName": "Doctor/facility name if present, else 'Unknown Doctor'",
  "reportReason": "Primary reason, exam/test name or focus",
  "summary": "Patient-friendly narrative that mirrors the example style below",
  "warnings": ["Important warnings/red flags/notes if present"]
}

Build the "summary" value as a clear narrative suitable for patients. If the report is a quantitative test, follow this structure when information is available:
- Patient: <patient name>
- Exam Date: <date>
- Main metric(s): include key parameters, patient result with unit, and a one-line layman interpretation
- Technical quality (if present)
- Overall Conclusion in plain English
- Add a final disclaimer: "This is a summary of the provided report. Discuss results with your doctor, who will interpret them in the context of your overall health."

Requirements:
- Keep explanations non-alarmist and in plain English.
- Include units and normal/abnormal interpretation when inferable.
- If specific fields are missing, omit those lines and still provide a sensible conclusion.
- Do NOT invent values; only interpret what is visible. If unsure, say so briefly.
- The JSON must be strictly valid.
""".strip()


@https_fn.on_call(
    timeout_sec=30,
    memory=options.MemoryOption.MB_512,
    secrets=["GEMINI_API_KEY"],
    enforce_app_check=False,
)
def validate_prescription(req: https_fn.CallableRequest) -> dict[str, bool]:
    payload = _require_image_payload(req.data)
    result = _generate_json_response(
        model=PRESCRIPTION_MODEL,
        prompt=PRESCRIPTION_VALIDATION_PROMPT,
        image_base64=payload["imageBase64"],
        mime_type=payload["mimeType"],
    )
    return {"valid": _as_bool(result.get("valid"))}


@https_fn.on_call(
    timeout_sec=60,
    memory=options.MemoryOption.GB_1,
    secrets=["GEMINI_API_KEY"],
    enforce_app_check=False,
)
def summarize_prescription(req: https_fn.CallableRequest) -> dict[str, Any]:
    payload = _require_image_payload(req.data)
    summary = _generate_json_response(
        model=PRESCRIPTION_MODEL,
        prompt=PRESCRIPTION_SUMMARY_PROMPT,
        image_base64=payload["imageBase64"],
        mime_type=payload["mimeType"],
    )
    return {"summary": _normalize_prescription_summary(summary)}


@https_fn.on_call(
    timeout_sec=30,
    memory=options.MemoryOption.MB_512,
    secrets=["GEMINI_API_KEY"],
    enforce_app_check=False,
)
def validate_medical_report(req: https_fn.CallableRequest) -> dict[str, bool]:
    payload = _require_image_payload(req.data)
    result = _generate_json_response(
        model=MEDICAL_REPORT_MODEL,
        prompt=MEDICAL_REPORT_VALIDATION_PROMPT,
        image_base64=payload["imageBase64"],
        mime_type=payload["mimeType"],
    )
    return {"valid": _as_bool(result.get("valid"))}


@https_fn.on_call(
    timeout_sec=60,
    memory=options.MemoryOption.GB_1,
    secrets=["GEMINI_API_KEY"],
    enforce_app_check=False,
)
def summarize_medical_report(req: https_fn.CallableRequest) -> dict[str, Any]:
    payload = _require_image_payload(req.data)
    summary = _generate_json_response(
        model=MEDICAL_REPORT_MODEL,
        prompt=MEDICAL_REPORT_SUMMARY_PROMPT,
        image_base64=payload["imageBase64"],
        mime_type=payload["mimeType"],
    )
    return {"summary": _normalize_medical_report_summary(summary)}


def _require_image_payload(data: Any) -> dict[str, str]:
    if not isinstance(data, dict):
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            message="Image payload is required.",
        )

    image_base64 = _as_string(data.get("imageBase64"))
    if not image_base64:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            message="imageBase64 is required.",
        )

    mime_type = _as_string(data.get("mimeType")) or DEFAULT_IMAGE_MIME_TYPE
    return {
        "imageBase64": image_base64,
        "mimeType": mime_type,
    }


def _generate_json_response(
    *,
    model: str,
    prompt: str,
    image_base64: str,
    mime_type: str,
) -> dict[str, Any]:
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
            message="GEMINI_API_KEY secret is not configured.",
        )

    url = f"{GEMINI_API_URL}/{model}:generateContent?key={api_key}"
    payload = {
        "contents": [
            {
                "role": "user",
                "parts": [
                    {
                        "inline_data": {
                            "mime_type": mime_type,
                            "data": image_base64,
                        }
                    },
                    {"text": prompt},
                ],
            }
        ],
        "generationConfig": {
            "responseMimeType": "application/json",
        },
    }

    try:
        response = requests.post(url, json=payload, timeout=60)
    except requests.RequestException as exc:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.UNAVAILABLE,
            message=f"Gemini request failed: {exc}",
        ) from exc

    response_body = response.json()
    if not response.ok:
        message = (
            response_body.get("error", {}).get("message")
            if isinstance(response_body, dict)
            else None
        ) or "Gemini request failed."
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message=message,
        )

    text = _extract_candidate_text(response_body)
    if not text:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message="Gemini returned an empty response.",
        )

    try:
        return json.loads(_strip_markdown_fences(text))
    except json.JSONDecodeError as exc:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message=f"Failed to parse Gemini JSON response: {exc}",
        ) from exc


def _extract_candidate_text(response_body: Any) -> str:
    if not isinstance(response_body, dict):
        return ""

    candidates = response_body.get("candidates")
    if not isinstance(candidates, list) or not candidates:
        return ""

    candidate = candidates[0]
    if not isinstance(candidate, dict):
        return ""

    content = candidate.get("content")
    if not isinstance(content, dict):
        return ""

    parts = content.get("parts")
    if not isinstance(parts, list) or not parts:
        return ""

    first_part = parts[0]
    if not isinstance(first_part, dict):
        return ""

    return _as_string(first_part.get("text"))


def _normalize_prescription_summary(value: Any) -> dict[str, Any]:
    data = value if isinstance(value, dict) else {}
    return {
        "doctorName": _as_string(data.get("doctorName")) or "Unknown Doctor",
        "medications": _as_medication_list(data.get("medications")),
        "dosageInstructions": _as_string_list(data.get("dosageInstructions")),
        "summary": _as_string(data.get("summary")),
        "warnings": _as_string_list(data.get("warnings")),
        "prescriptionReason": _as_string(data.get("prescriptionReason")),
        "stepsToCure": _as_string_list(data.get("stepsToCure")),
    }


def _normalize_medical_report_summary(value: Any) -> dict[str, Any]:
    data = value if isinstance(value, dict) else {}
    return {
        "doctorName": _as_string(data.get("doctorName")) or "Unknown Doctor",
        "summary": _as_string(data.get("summary")),
        "warnings": _as_string_list(data.get("warnings")),
        "reportReason": _as_string(data.get("reportReason")),
        "medications": _as_medication_list(data.get("medications")),
        "dosageInstructions": _as_string_list(data.get("dosageInstructions")),
        "stepsToCure": _as_string_list(data.get("stepsToCure")),
    }


def _as_medication_list(value: Any) -> list[dict[str, str]]:
    if not isinstance(value, list):
        return []

    items: list[dict[str, str]] = []
    for item in value:
        if not isinstance(item, dict):
            continue
        name = _as_string(item.get("name"))
        if not name:
            continue
        items.append(
            {
                "name": name,
                "dosage": _as_string(item.get("dosage")),
                "frequency": _as_string(item.get("frequency")),
                "duration": _as_string(item.get("duration")),
            }
        )
    return items


def _as_string_list(value: Any) -> list[str]:
    if not isinstance(value, list):
        return []
    return [_as_string(item) for item in value if _as_string(item)]


def _as_bool(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        return value.strip().lower() == "true"
    return False


def _as_string(value: Any) -> str:
    return value.strip() if isinstance(value, str) else ""


def _strip_markdown_fences(text: str) -> str:
    return text.replace("```json", "").replace("```", "").strip()
