package com.aritradas.medai.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class FeatureRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("featureDetail")
    val featureDetail: String,

    @SerializedName("timestamp")
    val timestamp: String
)

@Serializable
data class GoogleSheetsRequest(
    val values: List<List<String>>
)
