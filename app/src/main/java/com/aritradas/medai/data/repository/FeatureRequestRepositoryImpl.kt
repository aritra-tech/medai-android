package com.aritradas.medai.data.repository

import com.aritradas.medai.domain.model.FeatureRequest
import com.aritradas.medai.domain.repository.FeatureRequestRepository
import com.aritradas.medai.network.GoogleSheetsService
import com.aritradas.medai.utils.Resource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureRequestRepositoryImpl @Inject constructor(
    private val googleSheetsService: GoogleSheetsService
) : FeatureRequestRepository {

    override suspend fun submitFeatureRequest(featureRequest: FeatureRequest): Resource<String> {
        return try {
            val response = googleSheetsService.submitFeatureRequest(featureRequest)

            if (response.isSuccessful) {
                Resource.Success("Request submitted successfully!")
            } else {
                Timber.e("Failed to submit feature request: ${response.errorBody()?.string()}")
                Resource.Error("Failed to submit feature request. Please try again.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error submitting feature request")
            Resource.Error("Network error. Please check your connection and try again.")
        }
    }
}