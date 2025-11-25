package com.ai.vis.domain.usecase

import com.ai.vis.data.PhotoRepository

class UpdatePhotoFileNameUseCase(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(photoId: Long, newFileName: String) {
        photoRepository.updatePhotoFileName(photoId, newFileName)
    }
}
