package com.ai.vis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ai.vis.data.PhotoRepository
import com.ai.vis.data.SettingsDataStore
import com.ai.vis.domain.usecase.ClearAllDataUseCase
import com.ai.vis.domain.usecase.GetSettingsUseCase
import com.ai.vis.domain.usecase.UpdateLanguageUseCase
import com.ai.vis.domain.usecase.UpdateThemeUseCase

class SettingsViewModelFactory(
    private val settingsDataStore: SettingsDataStore,
    private val photoRepository: PhotoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val getSettingsUseCase = GetSettingsUseCase(settingsDataStore)
            val updateThemeUseCase = UpdateThemeUseCase(settingsDataStore)
            val updateLanguageUseCase = UpdateLanguageUseCase(settingsDataStore)
            val clearAllDataUseCase = ClearAllDataUseCase(photoRepository, settingsDataStore)
            
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                getSettingsUseCase,
                updateThemeUseCase,
                updateLanguageUseCase,
                clearAllDataUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
