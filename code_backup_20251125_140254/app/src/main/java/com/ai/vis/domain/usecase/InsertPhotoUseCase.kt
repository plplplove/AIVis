package com.ai.vis.domain.usecase

import com.ai.vis.data.EditedPhoto
import com.ai.vis.data.PhotoRepository

class InsertPhotoUseCase(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(photo: EditedPhoto): Long {
        return photoRepository.insertPhoto(photo)
    }
}
