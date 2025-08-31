package com.aritradas.medai.domain.repository

import com.aritradas.medai.domain.model.FeatureRequest
import com.aritradas.medai.utils.Resource

interface FeatureRequestRepository {
    suspend fun submitFeatureRequest(featureRequest: FeatureRequest): Resource<String>
}