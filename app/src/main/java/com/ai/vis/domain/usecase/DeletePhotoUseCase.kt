package com.ai.vis.domain.usecase

import com.ai.vis.data.EditedPhoto
import com.ai.vis.data.PhotoRepository
import java.io.File

class DeletePhotoUseCase(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(photo: EditedPhoto) {
        try {
            val file = File(photo.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
        }
        photoRepository.deletePhoto(photo)
    }
    
    suspend fun byId(photoId: Long) {
        photoRepository.deletePhotoById(photoId)
    }
}
