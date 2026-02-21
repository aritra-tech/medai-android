package com.aritradas.medai.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PillIdentification(
    val name: String,
    val confidence: Float,
    val color: String,
    val shape: String,
    val imprint: String?,
    val dosage: String?,
    val description: String,
    val medicalDisclaimer: String = "This identification is powered by AI and should not be used as the sole basis for taking medication. Consult a healthcare professional."
)

data class TFLitePillMatch(
    val color: String,
    val shape: String,
    val confidence: Float
)
