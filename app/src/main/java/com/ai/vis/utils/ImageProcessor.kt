package com.ai.vis.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class for image processing operations
 */
object ImageProcessor {
    
    /**
     * Load bitmap from URI
     */
    suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false) // Disable hardware bitmaps for processing
                .build()
            
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Rotate bitmap by 90 degrees
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Flip bitmap horizontally
     */
    fun flipBitmapHorizontal(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Flip bitmap vertically
     */
    fun flipBitmapVertical(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Crop bitmap with specific screen coordinates
     * @param cropRect: Screen coordinates of crop area
     * @param imageBounds: Screen bounds where image is displayed (with ContentScale.Fit)
     */
    fun cropBitmapWithRect(
        bitmap: Bitmap,
        cropRect: androidx.compose.ui.geometry.Rect,
        imageBounds: androidx.compose.ui.geometry.Rect
    ): Bitmap {
        // Convert screen coordinates to bitmap coordinates
        val scaleX = bitmap.width / imageBounds.width
        val scaleY = bitmap.height / imageBounds.height
        
        val bitmapCropRect = android.graphics.Rect(
            ((cropRect.left - imageBounds.left) * scaleX).toInt().coerceIn(0, bitmap.width),
            ((cropRect.top - imageBounds.top) * scaleY).toInt().coerceIn(0, bitmap.height),
            ((cropRect.right - imageBounds.left) * scaleX).toInt().coerceIn(0, bitmap.width),
            ((cropRect.bottom - imageBounds.top) * scaleY).toInt().coerceIn(0, bitmap.height)
        )
        
        val width = (bitmapCropRect.right - bitmapCropRect.left).coerceAtLeast(1)
        val height = (bitmapCropRect.bottom - bitmapCropRect.top).coerceAtLeast(1)
        
        return Bitmap.createBitmap(
            bitmap,
            bitmapCropRect.left,
            bitmapCropRect.top,
            width,
            height
        )
    }
    
    /**
     * Crop bitmap to specific ratio
     * @param ratio: null for free crop, or width/height ratio (e.g., 1.0f for 1:1, 1.333f for 4:3)
     */
    fun cropBitmap(bitmap: Bitmap, ratio: Float?): Bitmap {
        if (ratio == null) return bitmap // Free crop, return original
        
        val originalWidth = bitmap.width.toFloat()
        val originalHeight = bitmap.height.toFloat()
        val originalRatio = originalWidth / originalHeight
        
        return if (originalRatio > ratio) {
            // Image is wider than target ratio, crop width
            val newWidth = (originalHeight * ratio).toInt()
            val x = ((originalWidth - newWidth) / 2).toInt()
            Bitmap.createBitmap(bitmap, x, 0, newWidth, bitmap.height)
        } else {
            // Image is taller than target ratio, crop height
            val newHeight = (originalWidth / ratio).toInt()
            val y = ((originalHeight - newHeight) / 2).toInt()
            Bitmap.createBitmap(bitmap, 0, y, bitmap.width, newHeight)
        }
    }
    
    /**
     * Apply brightness adjustment (-1.0 to 1.0)
     */
    fun adjustBrightness(bitmap: Bitmap, value: Float): Bitmap {
        val colorMatrix = ColorMatrix().apply {
            val brightness = value * 255
            set(floatArrayOf(
                1f, 0f, 0f, 0f, brightness,
                0f, 1f, 0f, 0f, brightness,
                0f, 0f, 1f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, colorMatrix)
    }
    
    /**
     * Apply contrast adjustment (-1.0 to 1.0)
     */
    fun adjustContrast(bitmap: Bitmap, value: Float): Bitmap {
        val contrast = value + 1f // Convert to 0-2 range
        val translate = (-.5f * contrast + .5f) * 255f
        
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, colorMatrix)
    }
    
    /**
     * Apply saturation adjustment (-1.0 to 1.0)
     */
    fun adjustSaturation(bitmap: Bitmap, value: Float): Bitmap {
        val saturation = value + 1f // Convert to 0-2 range
        val colorMatrix = ColorMatrix().apply {
            setSaturation(saturation)
        }
        return applyColorMatrix(bitmap, colorMatrix)
    }
    
    /**
     * Apply temperature adjustment (-1.0 to 1.0)
     * Warm (positive) = more red/yellow, Cool (negative) = more blue
     */
    fun adjustTemperature(bitmap: Bitmap, value: Float): Bitmap {
        val colorMatrix = ColorMatrix().apply {
            val warm = value * 50 // Scale factor for temperature
            set(floatArrayOf(
                1f, 0f, 0f, 0f, warm,
                0f, 1f, 0f, 0f, warm * 0.5f,
                0f, 0f, 1f, 0f, -warm,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, colorMatrix)
    }
    
    /**
     * Apply tint adjustment (-1.0 to 1.0)
     * Green (positive) or Magenta (negative)
     */
    fun adjustTint(bitmap: Bitmap, value: Float): Bitmap {
        val colorMatrix = ColorMatrix().apply {
            val tint = value * 50
            set(floatArrayOf(
                1f, 0f, 0f, 0f, -tint,
                0f, 1f, 0f, 0f, tint,
                0f, 0f, 1f, 0f, -tint,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, colorMatrix)
    }
    
    /**
     * Apply sharpness (simplified version, -1.0 to 1.0)
     * Note: Real sharpness requires convolution matrix, this is a simplified version
     */
    fun adjustSharpness(bitmap: Bitmap, value: Float): Bitmap {
        if (value == 0f) return bitmap
        
        // For simplicity, we'll adjust contrast as a proxy for sharpness
        // A proper implementation would use convolution kernels
        val sharpness = 1f + (value * 0.5f)
        val translate = (-.5f * sharpness + .5f) * 255f
        
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                sharpness, 0f, 0f, 0f, translate,
                0f, sharpness, 0f, 0f, translate,
                0f, 0f, sharpness, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, colorMatrix)
    }
    
    /**
     * Apply color matrix to bitmap
     */
    private fun applyColorMatrix(bitmap: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    /**
     * Convert Bitmap to ImageBitmap for Compose
     */
    fun Bitmap.toImageBitmap(): ImageBitmap = this.asImageBitmap()
}
