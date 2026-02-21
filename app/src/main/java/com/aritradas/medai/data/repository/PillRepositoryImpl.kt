package com.aritradas.medai.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.aritradas.medai.BuildConfig
import com.aritradas.medai.data.local.TFLitePillAnalyzer
import com.aritradas.medai.domain.model.PillIdentification
import com.aritradas.medai.domain.repository.PillRepository
import com.aritradas.medai.utils.Resource
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_MEDICAL_DISCLAIMER =
    "This identification is powered by AI and should not be used as the sole basis for taking medication. Consult a healthcare professional."

@Singleton
class PillRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tfLitePillAnalyzer: TFLitePillAnalyzer
) : PillRepository {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val gson = Gson()

    override suspend fun identifyPill(imageUri: Uri): Resource<PillIdentification> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = uriToBitmap(imageUri)

                val localResult = tfLitePillAnalyzer.analyze(bitmap)

                val prompt = """
                    Analyze this image of a pill and provide detailed identification information.
                    If multiple matches are found, prioritize the most likely one.

                    ${localResult?.let {
                        "Local model hints (may be incomplete): color=${it.color}, shape=${it.shape}, confidence=${it.confidence}."
                    } ?: "No local model hints available."}
                    
                    Please respond ONLY with valid JSON in exactly this format:
                    {
                        "name": "Medication Name (e.g., Crocin, Lipitor)",
                        "confidence": 0.95,
                        "color": "Pill color(s)",
                        "shape": "Pill shape (e.g., Round, Oval, Capsule)",
                        "imprint": "Text or logos imprinted on the pill (if any)",
                        "dosage": "Typical dosage for this pill (e.g., 500mg)",
                        "description": "A brief, patient-friendly description of what this medication is for."
                    }
                    
                    Identification must be based on Color, Shape, and Imprint.
                    Include a high confidence score only if the imprint is clearly visible.
                """.trimIndent()

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text?.trim()?.removeSurrounding("```json", "```") ?: throw Exception("No response from Gemini")

                val dto = gson.fromJson(responseText, PillIdentificationDto::class.java)
                val result = PillIdentification(
                    name = dto.name?.takeIf { it.isNotBlank() } ?: "Unknown",
                    confidence = dto.confidence ?: 0f,
                    color = dto.color?.takeIf { it.isNotBlank() } ?: "Unknown",
                    shape = dto.shape?.takeIf { it.isNotBlank() } ?: "Unknown",
                    imprint = dto.imprint?.takeIf { it.isNotBlank() },
                    dosage = dto.dosage?.takeIf { it.isNotBlank() },
                    description = dto.description?.takeIf { it.isNotBlank() } ?: "No description provided.",
                    medicalDisclaimer = dto.medicalDisclaimer?.takeIf { it.isNotBlank() } ?: DEFAULT_MEDICAL_DISCLAIMER
                )
                Resource.Success(result)

            } catch (e: Exception) {
                Resource.Error("Failed to identify pill: ${e.message}")
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
}

private data class PillIdentificationDto(
    val name: String?,
    val confidence: Float?,
    val color: String?,
    val shape: String?,
    val imprint: String?,
    val dosage: String?,
    val description: String?,
    val medicalDisclaimer: String?
)
