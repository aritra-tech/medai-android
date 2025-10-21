package com.aritradas.medai.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}