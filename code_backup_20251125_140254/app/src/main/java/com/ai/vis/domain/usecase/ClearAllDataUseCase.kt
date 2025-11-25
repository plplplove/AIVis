package com.ai.vis.domain.usecase

import com.ai.vis.data.PhotoRepository
import com.ai.vis.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import java.io.File

class ClearAllDataUseCase(
    private val photoRepository: PhotoRepository,
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke() {
        try {
            val photoList = photoRepository.allPhotos.first()
            photoList.forEach { photo ->
                try {
                    val file = File(photo.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                }
            }
            
            photoRepository.deleteAllPhotos()
            
            settingsDataStore.updateTheme(false)
            settingsDataStore.updateLanguage("en")
            settingsDataStore.clearCache()
        } catch (e: Exception) {
        }
    }
}
