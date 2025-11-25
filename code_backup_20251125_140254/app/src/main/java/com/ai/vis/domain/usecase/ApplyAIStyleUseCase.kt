package com.ai.vis.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.ai.vis.ai.processor.AIStyleProcessor
import com.ai.vis.domain.model.AIStyle

class ApplyAIStyleUseCase(private val context: Context) {
    
    private val aiStyleProcessor = AIStyleProcessor(context)
    
    suspend operator fun invoke(bitmap: Bitmap, style: AIStyle): Bitmap {
        return aiStyleProcessor.applyStyle(bitmap, style)
    }
    
    fun release() {
        aiStyleProcessor.release()
    }
}
