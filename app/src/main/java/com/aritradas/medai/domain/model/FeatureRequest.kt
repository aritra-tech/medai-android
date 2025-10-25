package com.aritradas.medai.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FeatureRequest(
    val name: String,
    val email: String,
    val featureDetail: String,
    val timestamp: String
)

@Serializable
data class GoogleSheetsRequest(
    val values: List<List<String>>
)