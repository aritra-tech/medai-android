package com.aritradas.medai.ui.presentation.settings

import com.aritradas.medai.domain.model.ThemePreference

data class SettingsUiState(
    val biometricAuthEnabled: Boolean = false,
    val darkThemeEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val currentTheme: ThemePreference = ThemePreference.SYSTEM
)
