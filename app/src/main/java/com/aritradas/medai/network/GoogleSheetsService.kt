package com.aritradas.medai.network

import com.aritradas.medai.domain.model.FeatureRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GoogleSheetsService {
    @POST("exec")
    suspend fun submitFeatureRequest(
        @Body featureRequest: FeatureRequest
    ): Response<Any>
}