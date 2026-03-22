package com.aritradas.medai.ui.presentation.prescriptionSummarize

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritradas.medai.domain.model.DrugResult
import com.aritradas.medai.domain.model.SavedPrescription
import com.aritradas.medai.domain.repository.MedicineDetailsRepository
import com.aritradas.medai.domain.repository.PrescriptionRepository
import com.aritradas.medai.domain.repository.SummaryUsageRepository
import com.aritradas.medai.ui.presentation.prescriptionSummarize.state.PrescriptionUiState
import com.aritradas.medai.utils.ImageValidator
import com.aritradas.medai.utils.Resource
import com.aritradas.medai.utils.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PrescriptionSummarizeViewModel @Inject constructor(
    private val prescriptionRepository: PrescriptionRepository,
    private val medicineDetailsRepository: MedicineDetailsRepository,
    private val summaryUsageRepository: SummaryUsageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrescriptionUiState())
    val uiState: StateFlow<PrescriptionUiState> = _uiState.asStateFlow()

    private val _drugDetail = MutableStateFlow<DrugResult?>(null)
    val drugDetail = _drugDetail.asStateFlow()

    private val _isDrugLoading = MutableStateFlow(false)
    val isDrugLoading = _isDrugLoading.asStateFlow()

    private val _drugDetailError = MutableStateFlow<String?>(null)
    val drugDetailError = _drugDetailError.asStateFlow()

    private val _summaryUsageCount = MutableStateFlow(0)
    val summaryUsageCount = _summaryUsageCount.asStateFlow()

    private var onSaveSuccess: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            summaryUsageRepository.observeUsageCount().collect { usageCount ->
                _summaryUsageCount.value = usageCount
            }
        }

        viewModelScope.launch {
            summaryUsageRepository.syncUsageCount()
        }
    }

    fun setOnSaveSuccessCallback(callback: () -> Unit) {
        onSaveSuccess = callback
    }

    fun validateAndAnalyzePrescription(imageUri: Uri) {
        viewModelScope.launch {
            // First do basic image validation
            when (val basicValidation = ImageValidator.validateImageBasics(context, imageUri)) {
                is ValidationResult.Invalid -> {
                    _uiState.value = _uiState.value.copy(
                        validationError = basicValidation.message
                    )
                    return@launch
                }
                is ValidationResult.Warning -> {
                    // Continue with AI validation but could show warning
                    // For now, we'll proceed
                }
                ValidationResult.Valid -> {
                    // Continue with AI validation
                }
            }

            // Then validate with AI
            _uiState.value = _uiState.value.copy(
                isValidating = true,
                error = null,
                validationError = null,
                isValidPrescription = null
            )

            when (val validationResult = prescriptionRepository.validatePrescription(imageUri)) {
                is Resource.Success -> {
                    if (validationResult.data == true) {
                        _uiState.value = _uiState.value.copy(
                            isValidating = false,
                            isValidPrescription = true
                        )
                        // If valid, proceed with analysis
                        analyzePrescription(imageUri)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isValidating = false,
                            isValidPrescription = false,
                            validationError = "This image does not appear to be a valid medical prescription. Please upload a clear image of a doctor's prescription."
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        validationError = validationResult.message
                    )
                }
                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(
                        isValidating = true
                    )
                }
            }
        }
    }

    private fun analyzePrescription(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            when (val result = prescriptionRepository.summarizePrescription(imageUri)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        summary = result.data
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = true
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearValidationError() {
        _uiState.value = _uiState.value.copy(
            validationError = null,
            isValidPrescription = null
        )
    }

    fun clearSummary() {
        _uiState.value = _uiState.value.copy(
            summary = null,
            isValidPrescription = null,
            validationError = null
        )
    }

    fun incrementSummaryUsageCount() {
        viewModelScope.launch {
            val updatedUsage = summaryUsageRepository.incrementUsageCount()
            _summaryUsageCount.value = updatedUsage
        }
    }

    fun savePrescription() {
        val currentSummary = _uiState.value.summary ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                saveError = null,
                saveSuccess = false
            )

            // Create title using doctor's name
            val title =
                if (currentSummary.doctorName.isNotBlank() && currentSummary.doctorName != "Unknown Doctor") {
                    "${currentSummary.doctorName}'s prescription"
                } else {
                    val dateFormat =
                        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                    "Prescription - ${dateFormat.format(Date())}"
                }

            val savedPrescription = SavedPrescription(
                summary = currentSummary,
                title = title,
                savedAt = Date(),
                report = _uiState.value.report
            )

            when (val result = prescriptionRepository.savePrescription(savedPrescription)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                    // Trigger navigation callback
                    onSaveSuccess?.invoke()
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveError = result.message
                    )
                }

                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = true
                    )
                }
            }
        }
    }

    fun clearSaveStatus() {
        _uiState.value = _uiState.value.copy(
            saveSuccess = false,
            saveError = null
        )
    }

    fun updateReport(report: String) {
        _uiState.value = _uiState.value.copy(report = report)
    }

    fun clearReport() {
        _uiState.value = _uiState.value.copy(report = "")
    }

    fun submitReport() {
        val currentSummary = _uiState.value.summary ?: return
        val currentReport = _uiState.value.report

        if (currentReport.isBlank()) return

        viewModelScope.launch {
            // Create a temporary prescription with just the report to save it
            val title =
                if (currentSummary.doctorName.isNotBlank() && currentSummary.doctorName != "Unknown Doctor") {
                    "${currentSummary.doctorName}'s prescription - Report"
                } else {
                    val dateFormat =
                        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                    "Prescription Report - ${dateFormat.format(Date())}"
                }

            val prescriptionWithReport = SavedPrescription(
                summary = currentSummary,
                title = title,
                savedAt = Date(),
                report = currentReport
            )

            when (prescriptionRepository.savePrescription(prescriptionWithReport)) {
                is Resource.Success -> {
                    // Report saved successfully
                    clearReport()
                }

                is Resource.Error -> {
                    // Handle error if needed
                }

                is Resource.Loading -> {
                    // Handle loading if needed
                }
            }
        }
    }

    fun fetchDrugDetailByGenericName(medicineName: String) {
        viewModelScope.launch {
            _isDrugLoading.value = true
            _drugDetailError.value = null
            _drugDetail.value = null

            try {
                val result = medicineDetailsRepository.getDrugInfo(medicineName.trim())
                if (result != null) {
                    _drugDetail.value = result
                } else {
                    _drugDetailError.value = "Unable to fetch information for '$medicineName'. Please check the medicine name or consult a healthcare professional."
                }
            } catch (e: Exception) {
                _drugDetailError.value = "Failed to fetch medicine details. Please check your internet connection and try again."
                _drugDetail.value = null
            } finally {
                _isDrugLoading.value = false
            }
        }
    }
}
