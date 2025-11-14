package com.ai.vis.domain.usecase

import com.ai.vis.data.EditedPhoto
import com.ai.vis.data.PhotoRepository
import kotlinx.coroutines.flow.Flow

class GetAllPhotosUseCase(
    private val photoRepository: PhotoRepository
) {
    operator fun invoke(): Flow<List<EditedPhoto>> {
        return photoRepository.allPhotos
    }
}
