package com.ai.vis.ai.processor

import android.content.Context
import android.graphics.Bitmap
import com.ai.vis.ai.model.StyleTransferModel
import com.ai.vis.domain.model.AIStyle

class AIStyleProcessor(context: Context) {
    
    private val styleTransferModel = StyleTransferModel(context)
    
    suspend fun applyStyle(bitmap: Bitmap, style: AIStyle): Bitmap {
        if (style == AIStyle.NONE) {
            return bitmap
        }
        
        styleTransferModel.initialize(style)
        return styleTransferModel.applyStyle(bitmap)
    }
    
    fun release() {
        styleTransferModel.release()
    }
}
