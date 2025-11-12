package com.ai.vis.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidPath
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
     * Draw text on bitmap
     * @param textContent: Text to draw
     * @param textSize: Text size in sp
     * @param textColor: Text color
     * @param textPosition: Position in screen coordinates
     * @param imageBounds: Image bounds in screen coordinates
     * @param textAlign: Text alignment
     * @param isBold: Whether text is bold
     * @param hasStroke: Whether text has stroke/shadow
     * @param hasBackground: Whether text has background
     */
    fun drawTextOnBitmap(
        bitmap: Bitmap,
        context: android.content.Context,
        textContent: String,
        textSize: Float,
        textColor: Int,
        textPosition: androidx.compose.ui.geometry.Offset,
        imageBounds: androidx.compose.ui.geometry.Rect,
        textAlign: android.graphics.Paint.Align = android.graphics.Paint.Align.LEFT,
        isBold: Boolean = false,
        fontResourceId: Int? = null,
        letterSpacing: Float = 0f,
        isItalic: Boolean = false,
        isUnderline: Boolean = false,
        isStrikethrough: Boolean = false,
        hasStroke: Boolean = false,
        hasBackground: Boolean = false,
        textOpacity: Float = 1f,
        backgroundOpacity: Float = 0.7f,
        shadowRadius: Float = 0f,
        shadowOffsetX: Float = 0f,
        shadowOffsetY: Float = 0f,
        rotation: Float = 0f
    ): Bitmap {
        if (textContent.isEmpty()) return bitmap
        
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        // Convert screen coordinates to bitmap coordinates
        val scaleX = bitmap.width / imageBounds.width
        val scaleY = bitmap.height / imageBounds.height
        
        // Calculate bitmap position - TextField shows text from top-left
        val bitmapX = (textPosition.x - imageBounds.left) * scaleX
        val bitmapTextSize = textSize * scaleY
        
        val paint = Paint().apply {
            // Apply text opacity to the color
            val alpha = (textOpacity * 255).toInt().coerceIn(0, 255)
            color = (textColor and 0x00FFFFFF) or (alpha shl 24)
            this.textSize = bitmapTextSize
            this.textAlign = textAlign
            isAntiAlias = true
            
            // Apply font typeface
            typeface = when {
                fontResourceId != null -> {
                    // Load custom font from resources using androidx.core
                    try {
                        androidx.core.content.res.ResourcesCompat.getFont(context, fontResourceId)
                    } catch (e: Exception) {
                        // Fallback to default with style
                        var style = android.graphics.Typeface.NORMAL
                        if (isBold) style = style or android.graphics.Typeface.BOLD
                        if (isItalic) style = style or android.graphics.Typeface.ITALIC
                        android.graphics.Typeface.defaultFromStyle(style)
                    }
                }
                isBold && isItalic -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD_ITALIC)
                isBold -> android.graphics.Typeface.DEFAULT_BOLD
                isItalic -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                else -> android.graphics.Typeface.DEFAULT
            }
            
            // Apply letter spacing
            if (letterSpacing != 0f) {
                this.letterSpacing = letterSpacing * 0.1f
            }
            
            // Apply text decorations
            if (isUnderline) {
                flags = flags or Paint.UNDERLINE_TEXT_FLAG
            }
            if (isStrikethrough) {
                flags = flags or Paint.STRIKE_THRU_TEXT_FLAG
            }
            
            // Apply shadow if configured
            if (shadowRadius > 0f) {
                setShadowLayer(
                    shadowRadius * scaleY,
                    shadowOffsetX * scaleX,
                    shadowOffsetY * scaleY,
                    android.graphics.Color.argb(128, 0, 0, 0)
                )
            }
        }
        
        // Get text bounds to calculate proper positioning
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(textContent, 0, textContent.length, textBounds)
        
        // Adjust Y to draw text baseline (drawText draws from baseline, not top)
        // TextField position is top-left, so we need to add text height
        val bitmapY = (textPosition.y - imageBounds.top) * scaleY - textBounds.top
        
        // Clip canvas to bitmap bounds to prevent text from going outside
        canvas.save()
        canvas.clipRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        
        // Apply rotation if needed
        if (rotation != 0f) {
            canvas.rotate(rotation, bitmapX, bitmapY)
        }
        
        // Draw background if needed
        if (hasBackground) {
            // Calculate padding: 4.dp horizontal, 2.dp vertical
            val horizontalPaddingPx = 4f * scaleX
            val verticalPaddingPx = 2f * scaleY
            
            val bgAlpha = (backgroundOpacity * 255).toInt().coerceIn(0, 255)
            val bgPaint = Paint().apply {
                color = android.graphics.Color.argb(bgAlpha, 255, 255, 255)
                style = Paint.Style.FILL
            }
            
            val bgLeft = bitmapX - horizontalPaddingPx
            val bgTop = bitmapY + textBounds.top - verticalPaddingPx
            val bgRight = bitmapX + textBounds.width() + horizontalPaddingPx
            val bgBottom = bitmapY + textBounds.bottom + verticalPaddingPx
            
            canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, bgPaint)
        }
        
        // Draw stroke/shadow if needed
        if (hasStroke) {
            val strokePaint = Paint(paint).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f * scaleY
                color = android.graphics.Color.BLACK
            }
            canvas.drawText(textContent, bitmapX, bitmapY, strokePaint)
        }
        
        // Draw main text (will be clipped if outside bounds)
        canvas.drawText(textContent, bitmapX, bitmapY, paint)
        
        canvas.restore()
        
        return result
    }
    
    /**
     * Draw paths on bitmap
     * @param drawPaths: List of DrawPath objects
     * @param imageBounds: Image bounds in screen coordinates
     */
    fun drawPathsOnBitmap(
        bitmap: Bitmap,
        drawPaths: List<com.ai.vis.ui.components.DrawPath>,
        imageBounds: androidx.compose.ui.geometry.Rect
    ): Bitmap {
        if (drawPaths.isEmpty()) return bitmap
        
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        // Convert screen coordinates to bitmap coordinates
        val scaleX = bitmap.width / imageBounds.width
        val scaleY = bitmap.height / imageBounds.height
        
        // Створюємо окремий layer для малювання, щоб eraser стирав тільки малюнок
        val layerPaint = Paint()
        val layerBounds = android.graphics.RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        canvas.saveLayer(layerBounds, layerPaint)
        
        drawPaths.forEach { drawPath ->
            val paint = Paint().apply {
                strokeWidth = drawPath.strokeWidth * scaleY
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
                
                if (drawPath.isEraser) {
                    // Eraser mode - використовуємо DST_OUT для стирання тільки малюнка на layer
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
                    // Для eraser використовуємо чорний колір з повною непрозорістю
                    color = android.graphics.Color.BLACK
                } else {
                    // Normal drawing - встановлюємо колір
                    val alpha = (drawPath.opacity * 255).toInt().coerceIn(0, 255)
                    val r = (drawPath.color.red * 255).toInt()
                    val g = (drawPath.color.green * 255).toInt()
                    val b = (drawPath.color.blue * 255).toInt()
                    color = android.graphics.Color.argb(alpha, r, g, b)
                }
                
                // Apply blur/softness if needed
                if (drawPath.softness > 0f) {
                    // Softness from 0 to 1, convert to blur radius
                    // Max blur radius = strokeWidth / 2 for smooth edges
                    val blurRadius = drawPath.softness * (drawPath.strokeWidth * scaleY / 2)
                    if (blurRadius > 0f) {
                        maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                    }
                }
            }
            
            // Convert Compose Path to Android Path with coordinate scaling
            val composePath = drawPath.path
            val androidPath = android.graphics.Path()
            
            // Apply transformation matrix to scale and translate the path
            val matrix = android.graphics.Matrix()
            matrix.postTranslate(-imageBounds.left, -imageBounds.top)
            matrix.postScale(scaleX, scaleY)
            
            // Get the underlying Android path and transform it
            try {
                val srcPath = composePath.asAndroidPath()
                srcPath.transform(matrix, androidPath)
                canvas.drawPath(androidPath, paint)
            } catch (e: Exception) {
                android.util.Log.e("ImageProcessor", "Failed to draw path: ${e.message}")
            }
        }
        
        canvas.restore()
        
        return result
    }
    
    /**
     * Convert Bitmap to ImageBitmap for Compose
     */
    fun Bitmap.toImageBitmap(): ImageBitmap = this.asImageBitmap()
}
