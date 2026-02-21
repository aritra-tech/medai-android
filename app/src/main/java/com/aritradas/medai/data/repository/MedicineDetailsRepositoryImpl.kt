package com.aritradas.medai.data.repository

import android.R.attr.text
import com.aritradas.medai.BuildConfig
import com.aritradas.medai.domain.model.DrugResult
import com.aritradas.medai.domain.model.GeminiMedicineResponse
import com.aritradas.medai.domain.repository.MedicineDetailsRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineDetailsRepositoryImpl @Inject constructor() : MedicineDetailsRepository {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val gson = Gson()

    override suspend fun getDrugInfo(genericName: String): DrugResult? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Provide information about the medicine: "$genericName"

                Please respond ONLY with valid JSON in exactly this format (no extra text or markdown):

                {
                  "uses": [
                    "Short point describing a common use or condition this medicine treats",
                    "Another point, if applicable"
                  ],
                  "howItWorks": [
                    "Short, simple explanation of how this medicine works in the body"
                  ],
                  "benefits": [
                    "Point summarizing one key benefit",
                    "Another point, if needed"
                  ],
                  "sideEffects": [
                    "Common side effect (e.g., headache, nausea)",
                    "Serious side effect (e.g., chest pain) – when to call a doctor"
                  ]
                }

                Important:
                - Use simple, everyday language that anyone can understand.
                - Keep each bullet short and focused (1 sentence max).
                - Focus on common uses and effects in India and worldwide.
                - Mention common brand and generic names if useful.
                - If the medicine is a combination, briefly explain each ingredient.
                - If the name is unclear or partially wrong, give best-guess info based on similar medicines.
                - Be brief, clear, and helpful.
                - Do NOT include any extra text outside the JSON.
            """.trimIndent()

            val inputContent = content {
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            val responseText = response.text?.trim() ?: return@withContext null

            parseGeminiMedicineResponse(responseText, genericName)

        } catch (e: Exception) {
            null // Gracefully handle failures
        }
    }

    private fun parseGeminiMedicineResponse(responseText: String, medicineName: String): DrugResult? {
        return try {
            val cleanedResponse = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val geminiResponse = gson.fromJson(cleanedResponse, GeminiMedicineResponse::class.java)

            DrugResult(
                medicineName = medicineName,
                uses = geminiResponse.uses,
                howItWorks = geminiResponse.howItWorks,
                benefits = geminiResponse.benefits,
                sideEffects = geminiResponse.sideEffects
            )

        } catch (e: JsonSyntaxException) {
            parseFallbackMedicineResponse(responseText, medicineName)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseFallbackMedicineResponse(responseText: String, medicineName: String): DrugResult? {
        return try {
            val sections = responseText.split("\n").filter { it.isNotBlank() }

            var uses = listOf("Information not available")
            var howItWorks = listOf("Information not available")
            var benefits = listOf("Information not available")
            var sideEffects = listOf("Information not available")

            sections.forEach { line ->
                val lowerLine = line.lowercase()
                when {
                    lowerLine.contains("use") && lowerLine.contains(":") -> {
                        uses = line.substringAfter(":").split(Regex("[.•]")).map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    lowerLine.contains("work") && lowerLine.contains(":") -> {
                        howItWorks = line.substringAfter(":").split(Regex("[.•]")).map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    lowerLine.contains("benefit") && lowerLine.contains(":") -> {
                        benefits = line.substringAfter(":").split(Regex("[.•]")).map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    lowerLine.contains("side effect") && lowerLine.contains(":") -> {
                        sideEffects = line.substringAfter(":").split(Regex("[.•]")).map { it.trim() }.filter { it.isNotEmpty() }
                    }
                }
            }

            DrugResult(
                medicineName = medicineName,
                uses = uses.ifEmpty { listOf("Information not available") },
                howItWorks = howItWorks.ifEmpty { listOf("Information not available") },
                benefits = benefits.ifEmpty { listOf("Information not available") },
                sideEffects = sideEffects.ifEmpty { listOf("Information not available") }
            )
        } catch (e: Exception) {
            null
        }
    }
}