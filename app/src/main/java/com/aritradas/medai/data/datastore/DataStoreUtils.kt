package com.aritradas.medai.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aritradas.medai.domain.model.ThemePreference
import com.aritradas.medai.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
class DataStoreUtil @Inject constructor(context: Context) {

    val dataStore = context.dataStore
    val getDataStore get() = dataStore

    companion object {
        val IS_BIOMETRIC_AUTH_SET_KEY = booleanPreferencesKey("biometric_auth")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        
        private const val TAG = "DataStoreUtil"
    }

    suspend fun saveThemePreference(theme: ThemePreference): Resource<Unit> {
        return try {
            Timber.tag(TAG).d("Saving theme preference: ${theme.name}")
            dataStore.edit { prefs ->
                prefs[THEME_PREFERENCE] = theme.name
            }
            Timber.tag(TAG).d("Theme preference saved successfully: ${theme.name}")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error saving theme preference")
            Resource.Error<Unit>(e.message ?: "Error saving theme preference")
        }
    }

    fun getThemePreference(): Flow<ThemePreference> {
        Timber.tag(TAG).d("Getting theme preference")
        return dataStore.data
            .catch { e ->
                Timber.tag(TAG).e(e, "Error reading theme preference from DataStore")
                emit(emptyPreferences())
            }
            .map { prefs ->
                val themeName = prefs[THEME_PREFERENCE] ?: ThemePreference.SYSTEM.name
                ThemePreference.valueOf(themeName).also {
                    Timber.tag(TAG).d("Theme preference retrieved: ${it.name}")
                }
            }
    }
}
