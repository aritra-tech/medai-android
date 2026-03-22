package com.aritradas.medai.domain.repository

import kotlinx.coroutines.flow.Flow

interface SummaryUsageRepository {
    fun observeUsageCount(): Flow<Int>

    suspend fun syncUsageCount(): Int

    suspend fun incrementUsageCount(): Int
}
