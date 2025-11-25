package com.ai.vis.domain.usecase

import com.ai.vis.data.AppSettings
import com.ai.vis.data.SettingsDataStore
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(
    private val settingsDataStore: SettingsDataStore
) {
    operator fun invoke(): Flow<AppSettings> {
        return settingsDataStore.settingsFlow
    }
}
