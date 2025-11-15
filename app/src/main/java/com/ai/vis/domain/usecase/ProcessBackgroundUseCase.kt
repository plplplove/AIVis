package com.ai.vis.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.ai.vis.ai.processor.BackgroundProcessor
import com.ai.vis.ui.components.BackgroundOption

/**
 * Use case for background processing operations.
 * Handles remove, blur, and replace background operations.
 */
class ProcessBackgroundUseCase(private val context: Context) {
    
    private val backgroundProcessor = BackgroundProcessor(context)
    
    suspend fun initialize() {
        backgroundProcessor.initialize()
    }
    
    suspend operator fun invoke(
        bitmap: Bitmap,
        option: BackgroundOption,
        backgroundColor: Int? = null,
        backgroundImage: Bitmap? = null
    ): Bitmap {
        return when (option) {
            BackgroundOption.REMOVE -> {
                backgroundProcessor.removeBackground(bitmap)
            }
            BackgroundOption.BLUR -> {
                backgroundProcessor.blurBackground(bitmap, blurRadius = 25)
            }
            BackgroundOption.REPLACE -> {
                if (backgroundImage != null) {
                    backgroundProcessor.replaceBackgroundWithImage(bitmap, backgroundImage)
                } else {
                    // Use white background as default
                    val bgColor = backgroundColor ?: 0xFFFFFFFF.toInt()
                    backgroundProcessor.replaceBackground(bitmap, bgColor)
                }
            }
        }
    }
    
    fun release() {
        backgroundProcessor.release()
    }
}
