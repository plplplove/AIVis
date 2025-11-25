package com.ai.vis.domain.usecase

import android.graphics.Bitmap
import com.ai.vis.ui.components.FilterType
import com.ai.vis.utils.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApplyFilterUseCase {
    
    suspend operator fun invoke(
        bitmap: Bitmap,
        filterType: FilterType,
        intensity: Float = 1f
    ): Bitmap = withContext(Dispatchers.IO) {
        when (filterType) {
            FilterType.NONE -> bitmap
            FilterType.BW -> ImageProcessor.applyBWFilter(bitmap, intensity)
            FilterType.SEPIA -> ImageProcessor.applySepiaFilter(bitmap, intensity)
            FilterType.VINTAGE -> ImageProcessor.applyVintageFilter(bitmap, intensity)
            FilterType.COOL -> ImageProcessor.applyCoolFilter(bitmap, intensity)
            FilterType.WARM -> ImageProcessor.applyWarmFilter(bitmap, intensity)
            FilterType.GRAYSCALE -> ImageProcessor.applyGrayscaleFilter(bitmap, intensity)
            FilterType.INVERT -> ImageProcessor.applyInvertFilter(bitmap, intensity)
        }
    }
}
