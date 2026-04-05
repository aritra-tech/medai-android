package com.aritradas.medai.data.repository

import android.net.Uri
import com.aritradas.medai.data.remote.AiFunctionsClient
import com.aritradas.medai.domain.model.Medication
import com.aritradas.medai.domain.model.PrescriptionSummary
import com.aritradas.medai.domain.model.SavedPrescription
import com.aritradas.medai.domain.repository.PrescriptionRepository
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
class PrescriptionRepositoryImpl @Inject constructor(
    private val aiFunctionsClient: AiFunctionsClient
) : PrescriptionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun validatePrescription(imageUri: Uri): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Resource.Success(aiFunctionsClient.validatePrescription(imageUri))
            } catch (e: Exception) {
                Resource.Error("Failed to validate prescription: ${e.message}")
            }
        }
    }

    override suspend fun summarizePrescription(imageUri: Uri): Resource<PrescriptionSummary> {
        return withContext(Dispatchers.IO) {
            try {
                val summary = aiFunctionsClient
                    .summarizePrescription(imageUri)
                    .toPrescriptionSummary()
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

    private fun Map<*, *>.toPrescriptionSummary(): PrescriptionSummary {
        return PrescriptionSummary(
            doctorName = getString("doctorName") ?: "Unknown Doctor",
            medications = getMedicationList("medications"),
            dosageInstructions = getStringList("dosageInstructions"),
            summary = getString("summary") ?: "",
            warnings = getStringList("warnings"),
            prescriptionReason = getString("prescriptionReason") ?: "",
            stepsToCure = getStringList("stepsToCure")
        )
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
