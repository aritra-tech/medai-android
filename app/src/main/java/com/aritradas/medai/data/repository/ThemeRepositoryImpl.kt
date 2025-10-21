package com.aritradas.medai.data.repository

import com.aritradas.medai.data.datastore.DataStoreUtil
import com.aritradas.medai.domain.model.ThemePreference
import com.aritradas.medai.domain.repository.ThemeRepository
import com.aritradas.medai.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor(
    private val dataStoreUtil: DataStoreUtil
): ThemeRepository {

    override suspend fun setTheme(themePreference: ThemePreference): Resource<Unit> {
        return dataStoreUtil.saveThemePreference(themePreference)
    }

    override fun getTheme(): Flow<ThemePreference> {
        return dataStoreUtil.getThemePreference()
    }
}