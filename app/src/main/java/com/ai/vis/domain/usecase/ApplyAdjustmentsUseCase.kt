package com.ai.vis.domain.usecase

import android.graphics.Bitmap
import com.ai.vis.utils.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApplyAdjustmentsUseCase {
    
    suspend operator fun invoke(
        bitmap: Bitmap,
        brightness: Float = 0f,
        contrast: Float = 0f,
        saturation: Float = 0f,
        sharpness: Float = 0f,
        temperature: Float = 0f,
        tint: Float = 0f
    ): Bitmap = withContext(Dispatchers.IO) {
        var result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        
        if (brightness != 0f) {
            result = ImageProcessor.adjustBrightness(result, brightness)
        }
        
        if (contrast != 0f) {
            result = ImageProcessor.adjustContrast(result, contrast)
        }
        
        if (saturation != 0f) {
            result = ImageProcessor.adjustSaturation(result, saturation)
        }
        
        if (sharpness != 0f) {
            result = ImageProcessor.adjustSharpness(result, sharpness)
        }
        
        if (temperature != 0f) {
            result = ImageProcessor.adjustTemperature(result, temperature)
        }
        
        if (tint != 0f) {
            result = ImageProcessor.adjustTint(result, tint)
        }
        
        result
    }
}
