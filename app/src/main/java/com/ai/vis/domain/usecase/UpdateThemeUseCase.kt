package com.ai.vis.domain.usecase

import com.ai.vis.data.SettingsDataStore

class UpdateThemeUseCase(
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(isDarkTheme: Boolean) {
        settingsDataStore.updateTheme(isDarkTheme)
    }
}
