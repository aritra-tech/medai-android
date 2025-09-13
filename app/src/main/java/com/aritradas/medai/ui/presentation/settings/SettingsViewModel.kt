package com.aritradas.medai.ui.presentation.settings

import androidx.datastore.preferences.core.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritradas.medai.MainActivity
import com.aritradas.medai.data.datastore.DataStoreUtil
import com.aritradas.medai.domain.model.ThemePreference
import com.aritradas.medai.domain.repository.AuthRepository
import com.aritradas.medai.domain.repository.BiometricAuthListener
import com.aritradas.medai.domain.repository.ThemeRepository
import com.aritradas.medai.utils.AppBioMetricManager
import com.aritradas.medai.utils.Resource
import com.aritradas.medai.utils.runIO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appBioMetricManager: AppBioMetricManager,
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
    dataStoreUtil: DataStoreUtil
): ViewModel() {

    private val userDB = FirebaseFirestore.getInstance()

    private val dataStore = dataStoreUtil.dataStore

    val onLogOutComplete = MutableLiveData<Boolean>()

    val onDeleteAccountComplete = MutableLiveData<Boolean>()
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.data.map { preferences ->
                SettingsUiState(
                    biometricAuthEnabled = preferences[DataStoreUtil.IS_BIOMETRIC_AUTH_SET_KEY] ?: false
                )
            }.collect {
                _uiState.value = it
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            getTheme()
        }
    }

    fun getTheme() = runIO {
        themeRepository.getTheme().collectLatest { theme ->
            _uiState.update { it.copy(currentTheme = theme) }
        }
    }

    fun onThemeChanged(theme: ThemePreference) = runIO {
        when(themeRepository.setTheme(theme)) {
            is Resource.Success -> {
                _uiState.update { it.copy(currentTheme = theme) }
            }
            else -> {}
        }
    }
    fun showBiometricPrompt(activity: MainActivity) {
        appBioMetricManager.initBiometricPrompt(
            activity = activity,
            listener = object : BiometricAuthListener {
                override fun onBiometricAuthSuccess() {
                    viewModelScope.launch {
                        dataStore.edit { preferences ->
                            preferences[DataStoreUtil.IS_BIOMETRIC_AUTH_SET_KEY] =
                                !_uiState.value.biometricAuthEnabled
                        }
                    }
                }

                override fun onUserCancelled() {
                }

                override fun onErrorOccurred() {
                }
            }
        )
    }

    fun logout() = runIO {
        FirebaseAuth.getInstance().signOut()
        onLogOutComplete.postValue(true)
    }

    fun deleteAccount() = runIO {
        authRepository.getCurrentUser()?.let {
            userDB.collection("users").document(it.uid).delete()
        }
        onDeleteAccountComplete.postValue(true)
    }
}
