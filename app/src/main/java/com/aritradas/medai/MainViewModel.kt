package com.aritradas.medai

import androidx.lifecycle.ViewModel
import com.aritradas.medai.domain.model.ThemePreference
import com.aritradas.medai.domain.repository.ThemeRepository
import com.aritradas.medai.utils.runIO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
): ViewModel() {

    var themePreference = MutableStateFlow(ThemePreference.SYSTEM)
        private set

    init {
        getThemePreferences()
    }

    fun getThemePreferences() = runIO {
        themeRepository.getTheme().collectLatest {
            themePreference.value = it
        }
    }
}