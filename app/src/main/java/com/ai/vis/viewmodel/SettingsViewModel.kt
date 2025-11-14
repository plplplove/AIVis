package com.ai.vis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.vis.data.AppSettings
import com.ai.vis.domain.usecase.ClearAllDataUseCase
import com.ai.vis.domain.usecase.GetSettingsUseCase
import com.ai.vis.domain.usecase.UpdateLanguageUseCase
import com.ai.vis.domain.usecase.UpdateThemeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    getSettingsUseCase: GetSettingsUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase,
    private val updateLanguageUseCase: UpdateLanguageUseCase,
    private val clearAllDataUseCase: ClearAllDataUseCase
) : ViewModel() {

    val settings: StateFlow<AppSettings> = getSettingsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun toggleTheme() {
        viewModelScope.launch {
            val newThemeValue = !settings.value.isDarkTheme
            updateThemeUseCase(newThemeValue)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            updateLanguageUseCase(language)
        }
    }

    fun clearCache() {
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            clearAllDataUseCase()
        }
    }
}
