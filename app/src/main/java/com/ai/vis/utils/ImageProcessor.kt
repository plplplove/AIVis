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

object ImageProcessor {
    
    suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()
            
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    fun flipBitmap(bitmap: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) {
                postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            } else {
                postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
            }
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    fun cropBitmapWithRect(
        bitmap: Bitmap,
        cropRect: androidx.compose.ui.geometry.Rect,
        imageBounds: androidx.compose.ui.geometry.Rect
    ): Bitmap {
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
    
    fun cropBitmap(bitmap: Bitmap, ratio: Float?): Bitmap {
        if (ratio == null) return bitmap
        
        val originalWidth = bitmap.width.toFloat()
        val originalHeight = bitmap.height.toFloat()
        val originalRatio = originalWidth / originalHeight
        
        return if (originalRatio > ratio) {
            val newWidth = (originalHeight * ratio).toInt()
            val x = ((originalWidth - newWidth) / 2).toInt()
            Bitmap.createBitmap(bitmap, x, 0, newWidth, bitmap.height)
        } else {
            val newHeight = (originalWidth / ratio).toInt()
            val y = ((originalHeight - newHeight) / 2).toInt()
            Bitmap.createBitmap(bitmap, 0, y, bitmap.width, newHeight)
        }
    }
    
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
    
    fun adjustContrast(bitmap: Bitmap, value: Float): Bitmap {
        val contrast = value + 1f
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
    
    fun adjustSaturation(bitmap: Bitmap, value: Float): Bitmap {
        val saturation = value + 1f
        val colorMatrix = ColorMatrix().apply {
            setSaturation(saturation)
        }
        return applyColorMatrix(bitmap, colorMatrix)
    }
    
    fun adjustTemperature(bitmap: Bitmap, value: Float): Bitmap {
        val colorMatrix = ColorMatrix().apply {
            val warm = value * 50
            set(floatArrayOf(
                1f, 0f, 0f, 0f, warm,
                0f, 1f, 0f, 0f, warm * 0.5f,
                0f, 0f, 1f, 0f, -warm,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, colorMatrix)
    }
    
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
    
    fun applyVignette(bitmap: Bitmap, intensity: Float): Bitmap {
        if (intensity == 0f) return bitmap
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        val maxRadius = Math.sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()
        
        val paint = Paint().apply {
            isAntiAlias = true
            shader = android.graphics.RadialGradient(
                centerX,
                centerY,
                maxRadius,
                intArrayOf(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.argb((intensity * 200).toInt(), 0, 0, 0)
                ),
                floatArrayOf(
                    0.3f,
                    1.0f
                ),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        return result
    }
    
    fun applyBWFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(1f - intensity)
        return applyColorMatrixWithIntensity(bitmap, colorMatrix, intensity)
    }
    
    fun applySepiaFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrixWithIntensity(bitmap, colorMatrix, intensity)
    }
    
    fun applyVintageFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                0.6f, 0.3f, 0.1f, 0f, 30f * intensity,
                0.2f, 0.6f, 0.2f, 0f, 20f * intensity,
                0.2f, 0.3f, 0.5f, 0f, 10f * intensity,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrixWithIntensity(bitmap, colorMatrix, intensity)
    }
    
    fun applyCoolFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val coolMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                1f, 0f, 0f, 0f, -10f * intensity,
                0f, 1f, 0f, 0f, 10f * intensity,
                0f, 0f, 1f, 0f, 30f * intensity,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrixWithIntensity(bitmap, coolMatrix, intensity)
    }
    
    fun applyWarmFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val warmMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                1f, 0f, 0f, 0f, 30f * intensity,
                0f, 1f, 0f, 0f, 10f * intensity,
                0f, 0f, 1f, 0f, -20f * intensity,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrixWithIntensity(bitmap, warmMatrix, intensity)
    }
    
    fun applyGrayscaleFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(1f - intensity)
        return applyColorMatrixWithIntensity(bitmap, colorMatrix, intensity)
    }
    
    fun applyInvertFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val invertMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrixWithIntensity(bitmap, invertMatrix, intensity)
    }
    
    private fun applyColorMatrixWithIntensity(bitmap: Bitmap, colorMatrix: ColorMatrix, intensity: Float): Bitmap {
        if (intensity == 0f) return bitmap
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        if (intensity < 1f) {
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
            alpha = (intensity * 255).toInt()
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    fun adjustSharpness(bitmap: Bitmap, value: Float): Bitmap {
        if (value == 0f) return bitmap
        
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
    
    private fun applyColorMatrix(bitmap: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
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
        
        val scaleX = bitmap.width / imageBounds.width
        val scaleY = bitmap.height / imageBounds.height
        
        val bitmapX = (textPosition.x - imageBounds.left) * scaleX
        val bitmapTextSize = textSize * scaleY
        
        val paint = Paint().apply {
            val alpha = (textOpacity * 255).toInt().coerceIn(0, 255)
            color = (textColor and 0x00FFFFFF) or (alpha shl 24)
            this.textSize = bitmapTextSize
            this.textAlign = textAlign
            isAntiAlias = true
            
            typeface = when {
                fontResourceId != null -> {
                    try {
                        androidx.core.content.res.ResourcesCompat.getFont(context, fontResourceId)
                    } catch (e: Exception) {
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
            
            if (letterSpacing != 0f) {
                this.letterSpacing = letterSpacing * 0.1f
            }
            
            if (isUnderline) {
                flags = flags or Paint.UNDERLINE_TEXT_FLAG
            }
            if (isStrikethrough) {
                flags = flags or Paint.STRIKE_THRU_TEXT_FLAG
            }
            
            if (shadowRadius > 0f) {
                setShadowLayer(
                    shadowRadius * scaleY,
                    shadowOffsetX * scaleX,
                    shadowOffsetY * scaleY,
                    android.graphics.Color.argb(128, 0, 0, 0)
                )
            }
        }
        
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(textContent, 0, textContent.length, textBounds)
        
        val bitmapY = (textPosition.y - imageBounds.top) * scaleY - textBounds.top
        
        canvas.save()
        canvas.clipRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        
        if (rotation != 0f) {
            canvas.rotate(rotation, bitmapX, bitmapY)
        }
        
        if (hasBackground) {
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
        
        if (hasStroke) {
            val strokePaint = Paint(paint).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f * scaleY
                color = android.graphics.Color.BLACK
            }
            canvas.drawText(textContent, bitmapX, bitmapY, strokePaint)
        }
        
        canvas.drawText(textContent, bitmapX, bitmapY, paint)
        
        canvas.restore()
        
        return result
    }
    
    fun drawPathsOnBitmap(
        bitmap: Bitmap,
        drawPaths: List<com.ai.vis.ui.components.DrawPath>,
        imageBounds: androidx.compose.ui.geometry.Rect
    ): Bitmap {
        if (drawPaths.isEmpty()) return bitmap
        
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        val scaleX = bitmap.width / imageBounds.width
        val scaleY = bitmap.height / imageBounds.height
        
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
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
                    color = android.graphics.Color.BLACK
                } else {
                    val alpha = (drawPath.opacity * 255).toInt().coerceIn(0, 255)
                    val r = (drawPath.color.red * 255).toInt()
                    val g = (drawPath.color.green * 255).toInt()
                    val b = (drawPath.color.blue * 255).toInt()
                    color = android.graphics.Color.argb(alpha, r, g, b)
                }
                
                if (drawPath.softness > 0f) {
                    val blurRadius = drawPath.softness * (drawPath.strokeWidth * scaleY / 2)
                    if (blurRadius > 0f) {
                        maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                    }
                }
            }
            
            val composePath = drawPath.path
            val androidPath = android.graphics.Path()
            
            val matrix = android.graphics.Matrix()
            matrix.postTranslate(-imageBounds.left, -imageBounds.top)
            matrix.postScale(scaleX, scaleY)
            
            try {
                val srcPath = composePath.asAndroidPath()
                srcPath.transform(matrix, androidPath)
                canvas.drawPath(androidPath, paint)
            } catch (e: Exception) {
                            }
        }
        
        canvas.restore()
        
        return result
    }
    
    fun drawStickerOnBitmap(
        bitmap: Bitmap,
        emoji: String,
        position: androidx.compose.ui.geometry.Offset,
        emojiSizePx: Float,
        rotation: Float,
        opacity: Float,
        imageBounds: androidx.compose.ui.geometry.Rect
    ): Bitmap {
        if (emoji.isEmpty()) return bitmap
        
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        val scaleX = bitmap.width / imageBounds.width
        val scaleY = bitmap.height / imageBounds.height
        
        val bitmapEmojiSize = emojiSizePx * scaleY
        
        val paint = Paint().apply {
            textSize = bitmapEmojiSize
            textAlign = Paint.Align.LEFT  
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            alpha = (opacity * 255).toInt()
        }
        
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(emoji, 0, emoji.length, textBounds)
        
        val bitmapX = (position.x - imageBounds.left) * scaleX
        val bitmapY = (position.y - imageBounds.top) * scaleY - textBounds.top + (4f * scaleY)
        
        canvas.save()
        canvas.clipRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        
        if (rotation != 0f) {
            val centerX = bitmapX + textBounds.width() / 2f
            val centerY = bitmapY + textBounds.height() / 2f
            canvas.rotate(rotation, centerX, centerY)
        }
        
        canvas.drawText(emoji, bitmapX, bitmapY, paint)
        
        canvas.restore()
        
        return result
    }
    
    fun Bitmap.toImageBitmap(): ImageBitmap = this.asImageBitmap()
}
