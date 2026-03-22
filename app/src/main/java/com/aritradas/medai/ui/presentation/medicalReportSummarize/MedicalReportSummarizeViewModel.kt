package com.aritradas.medai.ui.presentation.medicalReportSummarize

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritradas.medai.domain.model.DrugResult
import com.aritradas.medai.domain.model.SavedMedicalReport
import com.aritradas.medai.domain.repository.MedicalReportRepository
import com.aritradas.medai.domain.repository.SummaryUsageRepository
import com.aritradas.medai.ui.presentation.medicalReportSummarize.state.MedicalReportUiState
import com.aritradas.medai.utils.ImageValidator
import com.aritradas.medai.utils.Resource
import com.aritradas.medai.utils.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MedicalReportSummarizeViewModel @Inject constructor(
    private val reportRepository: MedicalReportRepository,
    private val summaryUsageRepository: SummaryUsageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalReportUiState())
    val uiState: StateFlow<MedicalReportUiState> = _uiState.asStateFlow()

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

    fun validateAndAnalyzeReport(imageUri: Uri) {
        viewModelScope.launch {
            when (val basicValidation = ImageValidator.validateImageBasics(context, imageUri)) {
                is ValidationResult.Invalid -> {
                    _uiState.value = _uiState.value.copy(
                        validationError = basicValidation.message
                    )
                    return@launch
                }

                is ValidationResult.Warning -> {}
                ValidationResult.Valid -> {}
            }

            _uiState.value = _uiState.value.copy(
                isValidating = true,
                error = null,
                validationError = null,
                isValidReport = null
            )

            when (val validationResult = reportRepository.validateReport(imageUri)) {
                is Resource.Success -> {
                    if (validationResult.data == true) {
                        _uiState.value = _uiState.value.copy(
                            isValidating = false,
                            isValidReport = true
                        )
                        analyzeReport(imageUri)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isValidating = false,
                            isValidReport = false,
                            validationError = "This image does not appear to be a valid medical report. Please upload a clear image of a report or test result."
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

    private fun analyzeReport(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            when (val result = reportRepository.summarizeMedicalReport(imageUri)) {
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
            isValidReport = null
        )
    }

    fun clearSummary() {
        _uiState.value = _uiState.value.copy(
            summary = null,
            isValidReport = null,
            validationError = null
        )
    }

    fun incrementSummaryUsageCount() {
        viewModelScope.launch {
            val updatedUsage = summaryUsageRepository.incrementUsageCount()
            _summaryUsageCount.value = updatedUsage
        }
    }

    fun saveMedicalReport() {
        val currentSummary = _uiState.value.summary ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                saveError = null,
                saveSuccess = false
            )

            val reason = currentSummary.reportReason.takeIf {
                it.isNotBlank() && it.lowercase(Locale.getDefault()) != "not clearly visible"
            }
            val title = when {
                !reason.isNullOrBlank() -> "Medical Report - $reason"
                currentSummary.doctorName.isNotBlank() && currentSummary.doctorName != "Unknown Doctor" -> "${currentSummary.doctorName}'s report"
                else -> {
                    val dateFormat =
                        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                    "Medical Report - ${dateFormat.format(Date())}"
                }
            }

            val saved = SavedMedicalReport(
                summary = currentSummary,
                title = title,
                savedAt = Date(),
                report = _uiState.value.report
            )

            when (val result = reportRepository.saveMedicalReport(saved)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                    onSaveSuccess?.invoke()
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveError = result.message
                    )
                }

                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(isSaving = true)
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
            val reason = currentSummary.reportReason.takeIf {
                it.isNotBlank() && it.lowercase(Locale.getDefault()) != "not clearly visible"
            }
            val title = when {
                !reason.isNullOrBlank() -> "Medical Report - $reason"
                currentSummary.doctorName.isNotBlank() && currentSummary.doctorName != "Unknown Doctor" -> "${currentSummary.doctorName}'s report"
                else -> {
                    val dateFormat =
                        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                    "Medical Report - ${dateFormat.format(Date())}"
                }
            }

            val saved = SavedMedicalReport(
                summary = currentSummary,
                title = title,
                savedAt = Date(),
                report = currentReport
            )

            when (reportRepository.saveMedicalReport(saved)) {
                is Resource.Success -> {
                    clearReport()
                }

                is Resource.Error -> {}
                is Resource.Loading -> {}
            }
        }
    }
}
