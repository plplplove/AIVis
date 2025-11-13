package com.ai.vis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.vis.data.AppSettings
import com.ai.vis.data.PhotoRepository
import com.ai.vis.data.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val photoRepository: PhotoRepository
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
    
    fun clearAllData() {
        viewModelScope.launch {
            try {
                // Get all photos to delete their files
                val photos = photoRepository.allPhotos
                photos.collect { photoList ->
                    photoList.forEach { photo ->
                        try {
                            val file = File(photo.filePath)
                            if (file.exists()) {
                                file.delete()
                            }
                        } catch (e: Exception) {
                            Log.e("SettingsViewModel", "Error deleting file: ${photo.filePath}", e)
                        }
                    }
                    
                    // Delete all photos from database
                    photoRepository.deleteAllPhotos()
                    
                    // Reset settings to defaults
                    settingsDataStore.updateTheme(false) // Reset to light theme
                    settingsDataStore.updateLanguage("en") // Reset to English
                    settingsDataStore.clearCache()
                    
                    Log.d("SettingsViewModel", "All data cleared successfully")
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error clearing all data", e)
            }
        }
    }
}
