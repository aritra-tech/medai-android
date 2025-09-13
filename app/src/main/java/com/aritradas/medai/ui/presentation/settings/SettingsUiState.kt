package com.aritradas.medai.ui.presentation.settings

data class SettingsUiState(
    val biometricAuthEnabled: Boolean = false,
    val darkThemeEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true
)
