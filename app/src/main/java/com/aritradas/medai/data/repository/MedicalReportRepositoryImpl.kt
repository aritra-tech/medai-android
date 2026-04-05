package com.aritradas.medai.data.repository

import android.net.Uri
import com.aritradas.medai.data.remote.AiFunctionsClient
import com.aritradas.medai.domain.model.MedicalReportSummary
import com.aritradas.medai.domain.model.Medication
import com.aritradas.medai.domain.model.SavedMedicalReport
import com.aritradas.medai.domain.repository.MedicalReportRepository
import com.aritradas.medai.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicalReportRepositoryImpl @Inject constructor(
    private val aiFunctionsClient: AiFunctionsClient
) : MedicalReportRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun validateReport(imageUri: Uri): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Resource.Success(aiFunctionsClient.validateMedicalReport(imageUri))
            } catch (e: Exception) {
                Resource.Error("Failed to validate report: ${e.message}")
            }
        }
    }

    override suspend fun summarizeMedicalReport(imageUri: Uri): Resource<MedicalReportSummary> {
        return withContext(Dispatchers.IO) {
            try {
                val summary = aiFunctionsClient
                    .summarizeMedicalReport(imageUri)
                    .toMedicalReportSummary()
                Resource.Success(summary)
            } catch (e: Exception) {
                Resource.Error("Failed to analyze report: ${e.message}")
            }
        }
    }

    override suspend fun saveMedicalReport(report: SavedMedicalReport): Resource<String> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser
                    ?: return@withContext Resource.Error("User not authenticated. Please log in to save reports.")

                val reportData = hashMapOf(
                    "summary" to report.summary,
                    "savedAt" to report.savedAt,
                    "title" to report.title,
                    "report" to report.report,
                )

                val documentRef = firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .collection("reports")
                    .add(reportData)
                    .await()

                Resource.Success(documentRef.id)
            } catch (e: Exception) {
                Resource.Error("Failed to save report: ${e.message}")
            }
        }
    }

    override suspend fun getSavedMedicalReports(): Resource<List<SavedMedicalReport>> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser =
                    auth.currentUser ?: return@withContext Resource.Error("User not authenticated")

                val querySnapshot = firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .collection("reports")
                    .orderBy("savedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val reports = querySnapshot.documents.mapNotNull { document ->
                    try {
                        val data = document.data ?: return@mapNotNull null
                        val summaryMap =
                            data["summary"] as? Map<String, Any> ?: return@mapNotNull null

                        val summary = MedicalReportSummary(
                            doctorName = summaryMap["doctorName"] as? String ?: "Unknown Doctor",
                            summary = summaryMap["summary"] as? String ?: "",
                            warnings = (summaryMap["warnings"] as? List<String>) ?: emptyList(),
                        )

                        SavedMedicalReport(
                            id = document.id,
                            summary = summary,
                            savedAt = (data["savedAt"] as? com.google.firebase.Timestamp)?.toDate()
                                ?: java.util.Date(),
                            title = data["title"] as? String ?: "Untitled Report",
                            report = data["report"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                Resource.Success(reports)
            } catch (e: Exception) {
                Resource.Error("Failed to fetch reports: ${e.message}")
            }
        }
    }

    override suspend fun getMedicalReportById(id: String): Resource<SavedMedicalReport> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser =
                    auth.currentUser ?: return@withContext Resource.Error("User not authenticated")

                val document = firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .collection("reports")
                    .document(id)
                    .get()
                    .await()

                if (!document.exists()) {
                    return@withContext Resource.Error("Report not found")
                }

                val data = document.data ?: return@withContext Resource.Error("Invalid report data")
                val summaryMap = data["summary"] as? Map<String, Any>
                    ?: return@withContext Resource.Error("Invalid summary data")

                val summary = MedicalReportSummary(
                    doctorName = summaryMap["doctorName"] as? String ?: "Unknown Doctor",
                    summary = summaryMap["summary"] as? String ?: "",
                    warnings = (summaryMap["warnings"] as? List<String>) ?: emptyList()
                )

                val report = SavedMedicalReport(
                    id = document.id,
                    summary = summary,
                    savedAt = (data["savedAt"] as? com.google.firebase.Timestamp)?.toDate()
                        ?: java.util.Date(),
                    title = data["title"] as? String ?: "Untitled Report",
                    report = data["report"] as? String ?: ""
                )

                Resource.Success(report)
            } catch (e: Exception) {
                Resource.Error("Failed to fetch report: ${e.message}")
            }
        }
    }

    override suspend fun deleteMedicalReportById(id: String): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser =
                    auth.currentUser ?: return@withContext Resource.Error("User not authenticated")

                firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .collection("reports")
                    .document(id)
                    .delete()
                    .await()
                Resource.Success(true)
            } catch (e: Exception) {
                Resource.Error("Failed to delete report: ${e.message}")
            }
        }
    }

    private fun Map<*, *>.toMedicalReportSummary(): MedicalReportSummary {
        return MedicalReportSummary(
            doctorName = getString("doctorName") ?: "Unknown Doctor",
            medications = getMedicationList("medications"),
            dosageInstructions = getStringList("dosageInstructions"),
            summary = getString("summary") ?: "",
            warnings = getStringList("warnings"),
            reportReason = getString("reportReason") ?: "",
            stepsToCure = getStringList("stepsToCure")
        )
    }

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
