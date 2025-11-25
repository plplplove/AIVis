package com.ai.vis.domain.usecase

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApplyCropRotateUseCase {
    
    suspend fun crop(
        bitmap: Bitmap,
        cropRect: Rect
    ): Bitmap = withContext(Dispatchers.IO) {
        val x = cropRect.left.toInt().coerceIn(0, bitmap.width)
        val y = cropRect.top.toInt().coerceIn(0, bitmap.height)
        val width = cropRect.width.toInt().coerceIn(1, bitmap.width - x)
        val height = cropRect.height.toInt().coerceIn(1, bitmap.height - y)
        
        Bitmap.createBitmap(bitmap, x, y, width, height)
    }
    
    suspend fun rotate(
        bitmap: Bitmap,
        degrees: Float
    ): Bitmap = withContext(Dispatchers.IO) {
        if (degrees == 0f) return@withContext bitmap
        
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        
        Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
    
    suspend fun flip(
        bitmap: Bitmap,
        horizontal: Boolean = false,
        vertical: Boolean = false
    ): Bitmap = withContext(Dispatchers.IO) {
        if (!horizontal && !vertical) return@withContext bitmap
        
        val matrix = Matrix().apply {
            if (horizontal) {
                postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }
            if (vertical) {
                postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
            }
        }
        
        Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
}
