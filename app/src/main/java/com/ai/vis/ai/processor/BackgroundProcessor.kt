package com.ai.vis.ai.processor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import com.ai.vis.ai.model.BackgroundSegmentationModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackgroundProcessor(context: Context) {
    
    private val segmentationModel = BackgroundSegmentationModel(context)
    
    companion object {
    }
    
    suspend fun initialize() {
        segmentationModel.initialize()
    }
    
    suspend fun removeBackground(bitmap: Bitmap): Bitmap = withContext(Dispatchers.IO) {
        
        val mask = segmentationModel.segmentImage(bitmap)
        if (mask == null) {
            return@withContext bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        
        val refinedMask = segmentationModel.refineMask(mask, radius = 3)
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val idx = y * bitmap.width + x
                val pixel = pixels[idx]
                val maskValue = refinedMask[y][x]
                
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val alpha = (maskValue * 255).toInt().coerceIn(0, 255)
                
                pixels[idx] = (alpha shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        result
    }
    
    suspend fun blurBackground(bitmap: Bitmap, blurRadius: Int = 25): Bitmap = withContext(Dispatchers.IO) {
        
        val mask = segmentationModel.segmentImage(bitmap)
        if (mask == null) {
            return@withContext bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        
        val refinedMask = segmentationModel.refineMask(mask, radius = 3)
        
        val blurred = applyFastBlur(bitmap, blurRadius)
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val originalPixels = IntArray(bitmap.width * bitmap.height)
        val blurredPixels = IntArray(bitmap.width * bitmap.height)
        
        bitmap.getPixels(originalPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        blurred.getPixels(blurredPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val resultPixels = IntArray(bitmap.width * bitmap.height)
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val idx = y * bitmap.width + x
                val maskValue = refinedMask[y][x]
                
                val orig = originalPixels[idx]
                val blur = blurredPixels[idx]
                
                val or = (orig shr 16) and 0xFF
                val og = (orig shr 8) and 0xFF
                val ob = orig and 0xFF
                
                val br = (blur shr 16) and 0xFF
                val bg = (blur shr 8) and 0xFF
                val bb = blur and 0xFF
                
                val r = (or * maskValue + br * (1f - maskValue)).toInt().coerceIn(0, 255)
                val g = (og * maskValue + bg * (1f - maskValue)).toInt().coerceIn(0, 255)
                val b = (ob * maskValue + bb * (1f - maskValue)).toInt().coerceIn(0, 255)
                
                resultPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        result.setPixels(resultPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        blurred.recycle()
        
        result
    }
    
    suspend fun replaceBackground(
        bitmap: Bitmap, 
        backgroundColor: Int = 0xFFFFFFFF.toInt(),
        backgroundImage: Bitmap? = null
    ): Bitmap = withContext(Dispatchers.IO) {
        
        val mask = segmentationModel.segmentImage(bitmap)
        if (mask == null) {
            return@withContext bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        
        val refinedMask = segmentationModel.refineMask(mask, radius = 3)
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val bgR = (backgroundColor shr 16) and 0xFF
        val bgG = (backgroundColor shr 8) and 0xFF
        val bgB = backgroundColor and 0xFF
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val idx = y * bitmap.width + x
                val pixel = pixels[idx]
                val maskValue = refinedMask[y][x]
                
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                val finalR = (r * maskValue + bgR * (1f - maskValue)).toInt().coerceIn(0, 255)
                val finalG = (g * maskValue + bgG * (1f - maskValue)).toInt().coerceIn(0, 255)
                val finalB = (b * maskValue + bgB * (1f - maskValue)).toInt().coerceIn(0, 255)
                
                pixels[idx] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        result
    }
    
    suspend fun replaceBackgroundWithImage(
        foreground: Bitmap,
        backgroundImage: Bitmap
    ): Bitmap = withContext(Dispatchers.IO) {
        
        val mask = segmentationModel.segmentImage(foreground)
        if (mask == null) {
            return@withContext foreground.copy(Bitmap.Config.ARGB_8888, true)
        }
        
        val refinedMask = segmentationModel.refineMask(mask, radius = 3)
        
        val scaledBackground = Bitmap.createScaledBitmap(
            backgroundImage,
            foreground.width,
            foreground.height,
            true
        )
        
        val result = Bitmap.createBitmap(foreground.width, foreground.height, Bitmap.Config.ARGB_8888)
        val fgPixels = IntArray(foreground.width * foreground.height)
        val bgPixels = IntArray(foreground.width * foreground.height)
        
        foreground.getPixels(fgPixels, 0, foreground.width, 0, 0, foreground.width, foreground.height)
        scaledBackground.getPixels(bgPixels, 0, foreground.width, 0, 0, foreground.width, foreground.height)
        
        val resultPixels = IntArray(foreground.width * foreground.height)
        
        for (y in 0 until foreground.height) {
            for (x in 0 until foreground.width) {
                val idx = y * foreground.width + x
                val maskValue = refinedMask[y][x]
                
                val fg = fgPixels[idx]
                val bg = bgPixels[idx]
                
                val fr = (fg shr 16) and 0xFF
                val fg_g = (fg shr 8) and 0xFF
                val fb = fg and 0xFF
                
                val br = (bg shr 16) and 0xFF
                val bg_g = (bg shr 8) and 0xFF
                val bb = bg and 0xFF
                
                val r = (fr * maskValue + br * (1f - maskValue)).toInt().coerceIn(0, 255)
                val g = (fg_g * maskValue + bg_g * (1f - maskValue)).toInt().coerceIn(0, 255)
                val b = (fb * maskValue + bb * (1f - maskValue)).toInt().coerceIn(0, 255)
                
                resultPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        result.setPixels(resultPixels, 0, foreground.width, 0, 0, foreground.width, foreground.height)
        
        if (scaledBackground != backgroundImage) {
            scaledBackground.recycle()
        }
        
        result
    }
    
    private fun applyFastBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val factor = radius.coerceAtLeast(2)
        val downW = (bitmap.width / factor).coerceAtLeast(1)
        val downH = (bitmap.height / factor).coerceAtLeast(1)
        
        val down = Bitmap.createScaledBitmap(bitmap, downW, downH, true)
        
        val blurred = Bitmap.createScaledBitmap(down, bitmap.width, bitmap.height, true)
        
        if (down != bitmap) {
            down.recycle()
        }
        
        return blurred
    }
    
    fun release() {
        segmentationModel.release()
    }
}
