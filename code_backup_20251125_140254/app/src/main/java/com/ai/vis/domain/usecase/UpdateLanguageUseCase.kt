package com.ai.vis.domain.usecase

import com.ai.vis.data.SettingsDataStore

class UpdateLanguageUseCase(
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(languageCode: String) {
        settingsDataStore.updateLanguage(languageCode)
    }
}
