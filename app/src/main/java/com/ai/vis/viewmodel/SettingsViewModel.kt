package com.ai.vis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.vis.data.AppSettings
import com.ai.vis.data.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun toggleTheme() {
        viewModelScope.launch {
            val newThemeValue = !settings.value.isDarkTheme
            settingsDataStore.updateTheme(newThemeValue)
            Log.d("SettingsViewModel", "Theme changed to: ${if (newThemeValue) "Dark" else "Light"}")
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsDataStore.updateLanguage(language)
            Log.d("SettingsViewModel", "Language changed to: $language")
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            settingsDataStore.clearCache()
            Log.d("SettingsViewModel", "Cache cleared")
        }
    }
}
