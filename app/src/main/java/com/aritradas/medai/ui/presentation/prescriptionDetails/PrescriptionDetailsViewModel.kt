package com.aritradas.medai.ui.presentation.prescriptionDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritradas.medai.domain.repository.PrescriptionRepository
import com.aritradas.medai.ui.presentation.prescriptionDetails.state.PrescriptionDetailsUiState
import com.aritradas.medai.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrescriptionDetailsViewModel @Inject constructor(
    private val repository: PrescriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrescriptionDetailsUiState())
    val uiState: StateFlow<PrescriptionDetailsUiState> = _uiState.asStateFlow()

    fun loadPrescription(prescriptionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                prescription = null,
                isLoading = true,
                error = null
            )

            when (val result = repository.getPrescriptionById(prescriptionId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        prescription = result.data,
                        isLoading = false
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        prescription = null,
                        error = result.message,
                        isLoading = false
                    )
                }

                is Resource.Loading -> {
                    // Already handled by setting isLoading = true above
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun deletePrescription(prescriptionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.deletePrescriptionById(prescriptionId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isDeleted = true)
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        isDeleted = false
                    )
                }

                is Resource.Loading -> {
                    // Already marked as loading above
                }
            }
        }
    }
}
