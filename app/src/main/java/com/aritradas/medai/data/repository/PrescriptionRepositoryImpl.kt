package com.aritradas.medai.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.aritradas.medai.BuildConfig
import com.aritradas.medai.domain.model.GeminiPrescriptionResponse
import com.aritradas.medai.domain.model.Medication
import com.aritradas.medai.domain.model.PrescriptionSummary
import com.aritradas.medai.domain.model.SavedPrescription
import com.aritradas.medai.domain.repository.PrescriptionRepository
import com.aritradas.medai.utils.Resource
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrescriptionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PrescriptionRepository {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val gson = Gson()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun validatePrescription(imageUri: Uri): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = uriToBitmap(imageUri)
                val prompt = """
                    Analyze this image to determine if it contains a valid medical prescription from a doctor or healthcare provider.
                    
                    Look for these key indicators of a prescription:
                    1. Doctor's name, signature, or medical license number
                    2. Patient information
                    3. Medication names with proper dosages
                    4. Date of prescription
                    5. Pharmacy or clinic letterhead/stamp
                    6. Medical terminology and format
                    7. Rx symbol or prescription format
                    
                    Respond with ONLY "true" if this is clearly a medical prescription, or "false" if it's not.
                    
                    Consider it false if the image contains:
                    - Random text or documents
                    - Food items or general photos
                    - Screenshots of non-medical content
                    - Handwritten notes that aren't prescriptions
                    - Medicine boxes/bottles (these are not prescriptions)
                    - Generic medical information or articles
                """.trimIndent()

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text?.trim()?.lowercase() ?: "false"

                val isValid = responseText.contains("true")
                Resource.Success(isValid)

            } catch (e: Exception) {
                Resource.Error("Failed to validate prescription: ${e.message}")
            }
        }
    }

    override suspend fun summarizePrescription(imageUri: Uri): Resource<PrescriptionSummary> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = uriToBitmap(imageUri)
                val prompt = """
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
                            "presentingComplaints": "Short description of the problem (e.g., Varicocele)",
                            "provisionalDiagnosis": "Initial diagnosis or impression by the doctor",
                            "comorbidities": ["List any comorbid conditions mentioned, like diabetes or hypertension"],
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
                            "List of clear patient-friendly instructions based on the prescription. Examples: Apply cream locally, Take with food, Use support bandage"
                        ],
                        "prescriptionReason": "The main reason or medical condition for which this prescription was issued, e.g., Diabetes, Hypertension, Allergies. If not clear, use 'Not clearly visible' or infer based on diagnosis/medicines.",
                        "dosageInstructions": [
                          "Instructions related to how to take the medicine, e.g., Take after food, Do not crush"
                        ],
                        "warnings": [
                          "Any important warnings, precautions, or side effects mentioned or inferred based on the medicines"
                        ],
                        "summary": "Summarize the entire prescription in plain, easy-to-understand English. Include what the patient is suffering from, what medications are prescribed, for how long, how they should be taken, and any precautions to follow.",
                        "stepsToCure": [
                          "Step-by-step patient-friendly actionable steps needed to recover/cure, inferred from diagnosis, medications, and instructions (e.g., Follow full course of antibiotics; Take rest for X days; Apply cream as instructed; Maintain good hydration; Contact doctor if symptoms worsen). Whenever possible, make steps as actionable and clear as possible (3-7 steps typical). Include lifestyle advice if relevant. If not enough data, say 'Follow the general advice included in this prescription and consult your doctor for specific steps.'"
                        ]
                    }
                    If you cannot clearly read certain information, use "Not clearly visible" for that field.
                    For doctorName, look for signatures, printed names, letterheads, or any doctor identification. If found, format as "Dr. [Full Name]". If not clear, use "Unknown Doctor".
                    Ensure the medicine names exist and are valid (e.g., Chymoral Plus, Sporlac AF).
                    Translate any shorthand or symbols like "T-Back" into full medical names if possible.
                    Avoid medical jargon in the summary; use layman's terms.
                    Also make sure the medicines listed exists with the names. Make sure to validate all.
                    Include physical aids prescribed (e.g., bandages or support garments) in the instructions.
                    Ensure all JSON keys are present even if the arrays are empty.
                """.trimIndent()

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val responseText =
                    response.text?.trim() ?: throw Exception("No response from Gemini")

                // Parse the JSON response
                val summary = parseGeminiResponse(responseText)
                Resource.Success(summary)
            } catch (e: Exception) {
                Resource.Error("Failed to analyze prescription: ${e.message}")
            }
        }
    }

    override suspend fun savePrescription(prescription: SavedPrescription): Resource<String> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser

                if (currentUser == null) {
                    return@withContext Resource.Error("User not authenticated. Please log in to save prescriptions.")
                }

                val prescriptionData = hashMapOf(
                    "summary" to prescription.summary,
                    "savedAt" to prescription.savedAt,
                    "title" to prescription.title,
                    "report" to prescription.report,
                    "prescriptionReason" to prescription.summary.prescriptionReason,
                    "stepsToCure" to prescription.summary.stepsToCure
                )

                val documentRef = firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .collection("prescriptions")
                    .add(prescriptionData)
                    .await()

                Resource.Success(documentRef.id)
            } catch (e: Exception) {
                Resource.Error("Failed to save prescription: ${e.message}")
            }
        }
    }

    override suspend fun getSavedPrescriptions(): Resource<List<SavedPrescription>> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    return@withContext Resource.Error("User not authenticated")
                }

                val querySnapshot = firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .collection("prescriptions")
                    .orderBy("savedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val prescriptions = querySnapshot.documents.mapNotNull { document ->
                    try {
                        val data = document.data ?: return@mapNotNull null
                        parseSavedPrescription(document.id, data)
                    } catch (e: Exception) {
                        null // Skip malformed documents
                    }
                }

                Resource.Success(prescriptions)
            } catch (e: Exception) {
                Resource.Error("Failed to fetch prescriptions: ${e.message}")
            }
        }
    }

    override suspend fun getPrescriptionById(id: String): Resource<SavedPrescription> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    return@withContext Resource.Error("User not authenticated")
                }

                val document = firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .collection("prescriptions")
                    .document(id)
                    .get()
                    .await()

                if (!document.exists()) {
                    return@withContext Resource.Error("Prescription not found")
                }

                val data =
                    document.data ?: return@withContext Resource.Error("Invalid prescription data")
                val prescription = parseSavedPrescription(document.id, data)
                    ?: return@withContext Resource.Error("Invalid prescription data")

                Resource.Success(prescription)
            } catch (e: Exception) {
                Resource.Error("Failed to fetch prescription: ${e.message}")
            }
        }
    }

    override suspend fun deletePrescriptionById(id: String): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    return@withContext Resource.Error("User not authenticated")
                }
                firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .collection("prescriptions")
                    .document(id)
                    .delete()
                    .await()
                Resource.Success(true)
            } catch (e: Exception) {
                Resource.Error("Failed to delete prescription: ${e.message}")
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            throw Exception("Failed to load image: ${e.message}")
        }
    }

    private fun parseGeminiResponse(responseText: String): PrescriptionSummary {
        return try {
            val cleanedResponse = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // Parse JSON response
            val geminiResponse =
                gson.fromJson(cleanedResponse, GeminiPrescriptionResponse::class.java)

            // Convert to domain model
            PrescriptionSummary(
                doctorName = geminiResponse.doctorName,
                medications = geminiResponse.medications.map { medication ->
                    Medication(
                        name = medication.name,
                        dosage = medication.dosage,
                        frequency = medication.frequency,
                        duration = medication.duration
                    )
                },
                dosageInstructions = geminiResponse.dosageInstructions,
                summary = geminiResponse.summary,
                warnings = geminiResponse.warnings,
                prescriptionReason = geminiResponse.prescriptionReason,
                stepsToCure = geminiResponse.stepsToCure
            )

        } catch (e: JsonSyntaxException) {
            // Fallback parsing if JSON is malformed
            parseFallbackResponse(responseText)
        } catch (e: Exception) {
            // Return error state
            PrescriptionSummary(
                doctorName = "Unknown Doctor",
                medications = emptyList(),
                dosageInstructions = listOf("Could not parse prescription details"),
                summary = "Failed to analyze prescription image. Raw response: ${
                    responseText.take(
                        100
                    )
                }...",
                warnings = listOf("Please consult with a healthcare professional for accurate information"),
                prescriptionReason = "Parse error",
                stepsToCure = listOf("Unable to extract steps to cure. Please follow the doctor's advice and reach out for clarification.") // New fallback
            )
        }
    }

    private fun parseFallbackResponse(responseText: String): PrescriptionSummary {
        // Fallback parsing for when JSON parsing fails
        return PrescriptionSummary(
            doctorName = "Unknown Doctor",
            medications = extractMedicationsFromText(responseText),
            dosageInstructions = extractInstructionsFromText(responseText),
            summary = responseText.take(300) + if (responseText.length > 300) "..." else "",
            warnings = listOf("AI-generated summary - Please verify with healthcare professional"),
            prescriptionReason = "Could not extract reason",
            stepsToCure = listOf("General advice: Follow the given medications and instructions, and consult your doctor for a detailed treatment plan.") // fallback steps
        )
    }

    private fun extractMedicationsFromText(text: String): List<Medication> {
        // Simple text parsing for medications
        val medications = mutableListOf<Medication>()
        val lines = text.split("\n")

        lines.forEach { line ->
            // Look for medication patterns
            if (line.contains("mg", ignoreCase = true) ||
                line.contains("tablet", ignoreCase = true) ||
                line.contains("capsule", ignoreCase = true)
            ) {

                medications.add(
                    Medication(
                        name = line.take(50),
                        dosage = "As prescribed",
                        frequency = "As prescribed",
                        duration = "As prescribed"
                    )
                )
            }
        }

        return medications.ifEmpty {
            listOf(
                Medication(
                    name = "Could not extract medication names",
                    dosage = "Please refer to original prescription",
                    frequency = "Please refer to original prescription",
                    duration = "Please refer to original prescription"
                )
            )
        }
    }

    private fun extractInstructionsFromText(text: String): List<String> {
        val instructions = mutableListOf<String>()

        // Look for common instruction keywords
        val instructionKeywords = listOf("take", "with", "before", "after", "daily", "times")
        val lines = text.split("\n")

        lines.forEach { line ->
            if (instructionKeywords.any { keyword ->
                    line.contains(keyword, ignoreCase = true)
                }) {
                instructions.add(line.trim())
            }
        }

        return instructions.ifEmpty {
            listOf("Follow the instructions on the prescription")
        }
    }

    private fun parseSavedPrescription(
        documentId: String,
        data: Map<String, Any>
    ): SavedPrescription {
        val summaryMap = data["summary"].asMapOrNull()
        val medications = summaryMap?.getMedicationList("medications")
            ?.takeIf { it.isNotEmpty() }
            ?: data.getMedicationList("medications")
        val dosageInstructions = summaryMap?.getStringList("dosageInstructions")
            ?.takeIf { it.isNotEmpty() }
            ?: data.getStringList("dosageInstructions")
        val warnings = summaryMap?.getStringList("warnings")
            ?.takeIf { it.isNotEmpty() }
            ?: data.getStringList("warnings")
        val stepsToCure = summaryMap?.getStringList("stepsToCure")
            ?.takeIf { it.isNotEmpty() }
            ?: data.getStringList("stepsToCure")
        val report = summaryMap?.getString("report")
            ?: data.getString("report")
            ?: ""
        val summaryText = summaryMap?.getString("summary")
            ?: (data["summary"] as? String)
            ?: ""
        val doctorName = summaryMap?.getString("doctorName")
            ?: data.getString("doctorName")
            ?: "Unknown Doctor"
        val prescriptionReason = summaryMap?.getString("prescriptionReason")
            ?: data.getString("prescriptionReason")
            ?: ""

        val prescriptionSummary = PrescriptionSummary(
            doctorName = doctorName,
            medications = medications,
            dosageInstructions = dosageInstructions,
            summary = summaryText,
            warnings = warnings,
            prescriptionReason = prescriptionReason,
            report = report,
            stepsToCure = stepsToCure
        )

        return SavedPrescription(
            id = documentId,
            summary = prescriptionSummary,
            savedAt = (data["savedAt"] as? com.google.firebase.Timestamp)?.toDate()
                ?: java.util.Date(),
            title = data.getString("title") ?: "Untitled Prescription",
            report = report,
            prescriptionReason = prescriptionReason,
            stepsToCure = stepsToCure
        )
    }

    private fun Any?.asMapOrNull(): Map<*, *>? = this as? Map<*, *>

    private fun Map<*, *>.getString(key: String): String? =
        (this[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }

    private fun Map<*, *>.getStringList(key: String): List<String> =
        (this[key] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

    private fun Map<*, *>.getMedicationList(key: String): List<Medication> =
        (this[key] as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.toMedicationOrNull()
        } ?: emptyList()

    private fun Map<*, *>.toMedicationOrNull(): Medication? {
        val name = getString("name") ?: return null
        return Medication(
            name = name,
            dosage = getString("dosage") ?: "",
            frequency = getString("frequency") ?: "",
            duration = getString("duration") ?: ""
        )
    }
}
