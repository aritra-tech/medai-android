package com.aritradas.medai.domain.repository

import com.aritradas.medai.domain.model.ThemePreference
import com.aritradas.medai.utils.Resource
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {

    suspend fun setTheme(themePreference: ThemePreference): Resource<Unit>

    fun getTheme(): Flow<ThemePreference>
}