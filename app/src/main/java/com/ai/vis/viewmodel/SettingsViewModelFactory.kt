package com.ai.vis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ai.vis.data.PhotoRepository
import com.ai.vis.data.SettingsDataStore

class SettingsViewModelFactory(
    private val settingsDataStore: SettingsDataStore,
    private val photoRepository: PhotoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsDataStore, photoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
