package com.aritradas.medai.ui.presentation.pillIdentification

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritradas.medai.domain.model.PillIdentification
import com.aritradas.medai.domain.repository.PillRepository
import com.aritradas.medai.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PillIdentificationViewModel @Inject constructor(
    private val repository: PillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PillUiState>(PillUiState.Idle)
    val uiState: StateFlow<PillUiState> = _uiState.asStateFlow()

    fun identifyPill(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = PillUiState.Analyzing
            when (val result = repository.identifyPill(imageUri)) {
                is Resource.Success -> {
                    _uiState.value = PillUiState.Success(result.data!!)
                }
                is Resource.Error -> {
                    _uiState.value = PillUiState.Error(result.message ?: "Unknown error")
                }
                else -> {}
            }
        }
    }

    fun resetState() {
        _uiState.value = PillUiState.Idle
    }
}

sealed class PillUiState {
    data object Idle : PillUiState()
    data object Analyzing : PillUiState()
    data class Success(val pill: PillIdentification) : PillUiState()
    data class Error(val message: String) : PillUiState()
}
