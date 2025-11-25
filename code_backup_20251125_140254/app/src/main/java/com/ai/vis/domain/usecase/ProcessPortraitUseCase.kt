package com.ai.vis.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.ai.vis.ai.processor.PortraitProcessor
import com.ai.vis.ui.components.PortraitOption

class ProcessPortraitUseCase(private val context: Context) {
    
    private val portraitProcessor = PortraitProcessor(context)
    
    suspend fun initialize() {
        portraitProcessor.initialize()
    }
    
    suspend operator fun invoke(
        bitmap: Bitmap,
        option: PortraitOption,
        beautyIntensity: Float = 0.5f,
        eyeIntensity: Float = 0.5f,
        blurIntensity: Float = 0.5f
    ): Bitmap {
        return when (option) {
            PortraitOption.BEAUTY_MODE -> {
                portraitProcessor.applyBeautyMode(bitmap, beautyIntensity)
            }
            PortraitOption.EYE_ENHANCEMENT -> {
                portraitProcessor.enhanceEyes(bitmap, eyeIntensity)
            }
            PortraitOption.FACE_BLUR -> {
                portraitProcessor.blurFace(bitmap, blurIntensity)
            }
            PortraitOption.NONE -> bitmap
        }
    }
    
    fun release() {
        portraitProcessor.release()
    }
}
