package com.aritradas.medai.data.repository

import com.aritradas.medai.BuildConfig
import com.aritradas.medai.domain.model.FeatureRequest
import com.aritradas.medai.domain.model.GoogleSheetsRequest
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

    companion object {
        private const val SPREADSHEET_ID = BuildConfig.GOOGLE_SHEETS_ID
        private const val RANGE = "Sheet1!A:D"
        private const val GOOGLE_SHEETS_API_KEY = BuildConfig.GOOGLE_SHEETS_API_KEY
    }

    override suspend fun submitFeatureRequest(featureRequest: FeatureRequest): Resource<String> {
        return try {

            val request = GoogleSheetsRequest(
                values = listOf(
                    listOf(
                        featureRequest.name,
                        featureRequest.email,
                        featureRequest.featureDetail,
                        featureRequest.timestamp
                    )
                )
            )

            val response = googleSheetsService.appendValues(
                spreadsheetId = SPREADSHEET_ID,
                range = RANGE,
                apiKey = GOOGLE_SHEETS_API_KEY,
                request = request
            )

            if (response.isSuccessful) {
                Resource.Success("Feature request submitted successfully!")
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