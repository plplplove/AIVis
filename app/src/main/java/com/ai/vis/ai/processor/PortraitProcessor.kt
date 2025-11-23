package com.ai.vis.ai.processor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Processor for portrait enhancement operations:
 * - Beauty Mode: Skin smoothing and color correction
 * - Eye Enhancement: Brighten and enhance eye area
 * - Face Blur: Blur face region
 */
class PortraitProcessor(private val context: Context) {
    
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f) // Detect smaller faces
            .enableTracking()
            .build()
    )
    
    companion object {
        private const val TAG = "PortraitProcessor"
    }
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        // ML Kit Face Detection не потребує ініціалізації
        Log.d(TAG, "✅ ML Kit Face Detection ready")
    }
    
    /**
     * Detect face region in the image using ML Kit Face Detection.
     * Returns a Rect representing the face bounding box, or null if no face detected.
     */
    private suspend fun detectFace(bitmap: Bitmap): Rect? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Running ML Kit face detection on ${bitmap.width}x${bitmap.height} image")
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = faceDetector.process(image).await()
            
            Log.d(TAG, "ML Kit found ${faces.size} faces")
            
            if (faces.isNotEmpty()) {
                // Get the largest face
                val largestFace = faces.maxByOrNull { face -> 
                    face.boundingBox.width() * face.boundingBox.height() 
                }
                if (largestFace != null) {
                    val box = largestFace.boundingBox
                    Log.d(TAG, "✅ Face detected by ML Kit: $box (${faces.size} total faces)")
                    return@withContext box
                }
            }
            
            Log.d(TAG, "No face detected by ML Kit, using fallback")
            return@withContext getCenterFaceRegion(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting face: ${e.message}", e)
            return@withContext getCenterFaceRegion(bitmap)
        }
    }
    
    private fun getCenterFaceRegion(bitmap: Bitmap): Rect {
        // Smart fallback: detect face using skin tone heuristics
        Log.d(TAG, "Using smart fallback face detection")
        
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        var minX = bitmap.width
        var maxX = 0
        var minY = bitmap.height
        var maxY = 0
        var skinPixelCount = 0
        
        // Scan for skin-tone pixels
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = pixels[y * bitmap.width + x]
                if (isSkinTone(pixel)) {
                    minX = min(minX, x)
                    maxX = max(maxX, x)
                    minY = min(minY, y)
                    maxY = max(maxY, y)
                    skinPixelCount++
                }
            }
        }
        
        // If we found enough skin pixels, use that region
        if (skinPixelCount > (bitmap.width * bitmap.height * 0.05f)) {
            // Add padding around detected region
            val padding = ((maxX - minX) * 0.15f).toInt()
            val faceRect = Rect(
                max(0, minX - padding),
                max(0, minY - padding),
                min(bitmap.width, maxX + padding),
                min(bitmap.height, maxY + padding)
            )
            Log.d(TAG, "Skin-based face region: $faceRect (${skinPixelCount} skin pixels)")
            return faceRect
        }
        
        // Fallback to center region if skin detection fails
        Log.d(TAG, "Skin detection failed, using center region")
        val centerX = bitmap.width / 2
        val centerY = (bitmap.height * 0.35f).toInt()
        val faceWidth = (bitmap.width * 0.35f).toInt()
        val faceHeight = (bitmap.height * 0.25f).toInt()
        
        return Rect(
            max(0, centerX - faceWidth / 2),
            max(0, centerY - faceHeight / 2),
            min(bitmap.width, centerX + faceWidth / 2),
            min(bitmap.height, centerY + faceHeight / 2)
        )
    }
    
    private fun isSkinTone(pixel: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        
        // Skin tone detection heuristic (works for various skin tones)
        // Based on RGB ranges that typically represent human skin
        return (r > 95 && g > 40 && b > 20 &&
                r > g && r > b &&
                abs(r - g) > 15 &&
                r - g > 15) ||
               // Lighter skin tones
               (r > 220 && g > 210 && b > 170 &&
                abs(r - g) < 15 && r > b && g > b)
    }
    
    /**
     * Apply beauty mode: blemish removal, skin smoothing and color correction
     */
    suspend fun applyBeautyMode(bitmap: Bitmap, intensity: Float): Bitmap = withContext(Dispatchers.IO) {
        Log.d(TAG, "Applying beauty mode with intensity: $intensity")
        
        val faceRect = detectFace(bitmap)
        var result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        if (faceRect == null) {
            Log.w(TAG, "No face detected, applying to entire image")
            return@withContext applySkinSmoothing(result, intensity)
        }
        
        // Step 1: Remove blemishes and dark spots
        result = removeBlemishes(result, faceRect, intensity)
        
        // Step 2: Apply skin smoothing to face region
        result = applySkinSmoothingToRegion(result, faceRect, intensity)
        
        // Step 3: Brighten skin tone slightly (removed - was too aggressive)
        
        Log.d(TAG, "Beauty mode applied successfully")
        result
    }
    
    /**
     * Enhance eyes: brighten and increase contrast in eye area
     */
    suspend fun enhanceEyes(bitmap: Bitmap, intensity: Float): Bitmap = withContext(Dispatchers.IO) {
        Log.d(TAG, "Enhancing eyes with intensity: $intensity")
        
        val faceRect = detectFace(bitmap)
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        if (faceRect == null) {
            Log.w(TAG, "No face detected, skipping eye enhancement")
            return@withContext result
        }
        
        // Eyes are positioned closer together, in upper-middle portion
        // Left eye region (closer to center)
        val leftEyeRegion = Rect(
            faceRect.left + (faceRect.width() * 0.25f).toInt(),
            faceRect.top + (faceRect.height() * 0.30f).toInt(),
            faceRect.left + (faceRect.width() * 0.45f).toInt(),
            faceRect.top + (faceRect.height() * 0.50f).toInt()
        )
        
        // Right eye region (closer to center)
        val rightEyeRegion = Rect(
            faceRect.left + (faceRect.width() * 0.55f).toInt(),
            faceRect.top + (faceRect.height() * 0.30f).toInt(),
            faceRect.left + (faceRect.width() * 0.75f).toInt(),
            faceRect.top + (faceRect.height() * 0.50f).toInt()
        )
        
        // Enhance both eye regions
        var enhanced = brightenRegion(result, leftEyeRegion, intensity)
        enhanced = brightenRegion(enhanced, rightEyeRegion, intensity)
        
        Log.d(TAG, "Eye enhancement applied successfully")
        enhanced
    }
    
    /**
     * Blur face region
     */
    suspend fun blurFace(bitmap: Bitmap, intensity: Float): Bitmap = withContext(Dispatchers.IO) {
        Log.d(TAG, "Blurring face with intensity: $intensity")
        
        val faceRect = detectFace(bitmap)
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        if (faceRect == null) {
            Log.w(TAG, "No face detected, applying to center region")
        }
        
        // Expand face region by 30% for better coverage (especially width)
        val expandedRegion = if (faceRect != null) {
            val expansionWidth = (faceRect.width() * 0.35f).toInt()
            val expansionHeight = (faceRect.height() * 0.30f).toInt()
            Rect(
                max(0, faceRect.left - expansionWidth),
                max(0, faceRect.top - expansionHeight),
                min(bitmap.width, faceRect.right + expansionWidth),
                min(bitmap.height, faceRect.bottom + expansionHeight)
            )
        } else {
            // If no face detected, use larger center region
            val centerX = bitmap.width / 2
            val centerY = (bitmap.height * 0.35f).toInt()
            val faceWidth = (bitmap.width * 0.45f).toInt() // Increased from 0.35
            val faceHeight = (bitmap.height * 0.35f).toInt() // Increased from 0.25
            
            Rect(
                max(0, centerX - faceWidth / 2),
                max(0, centerY - faceHeight / 2),
                min(bitmap.width, centerX + faceWidth / 2),
                min(bitmap.height, centerY + faceHeight / 2)
            )
        }
        
        val blurred = applyBlurToRegion(result, expandedRegion, intensity)
        
        Log.d(TAG, "Face blur applied successfully")
        blurred
    }
    
    /**
     * Apply skin smoothing to entire image
     */
    private fun applySkinSmoothing(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        // Simple bilateral filter approximation
        val smoothed = applySimpleBilateralFilter(pixels, bitmap.width, bitmap.height, intensity)
        
        result.setPixels(smoothed, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    /**
     * Apply skin smoothing to specific region
     */
    private fun applySkinSmoothingToRegion(bitmap: Bitmap, region: Rect, intensity: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(bitmap.width * bitmap.height)
        result.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        // Apply smoothing only to face region
        for (y in region.top until region.bottom) {
            for (x in region.left until region.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val idx = y * bitmap.width + x
                    pixels[idx] = smoothPixel(pixels, bitmap.width, bitmap.height, x, y, intensity)
                }
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    private fun smoothPixel(pixels: IntArray, width: Int, height: Int, x: Int, y: Int, intensity: Float): Int {
        // Use smaller radius for more subtle smoothing
        val radius = (2 * intensity).toInt().coerceAtLeast(1)
        var r = 0f
        var g = 0f
        var b = 0f
        var count = 0
        
        val original = pixels[y * width + x]
        val origR = (original shr 16) and 0xFF
        val origG = (original shr 8) and 0xFF
        val origB = original and 0xFF
        
        // Gaussian-like weighted average
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val nx = x + dx
                val ny = y + dy
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    val distance = sqrt((dx * dx + dy * dy).toFloat())
                    val weight = 1f / (1f + distance) // Distance-based weight
                    
                    val pixel = pixels[ny * width + nx]
                    val pr = ((pixel shr 16) and 0xFF)
                    val pg = ((pixel shr 8) and 0xFF)
                    val pb = (pixel and 0xFF)
                    
                    // Only smooth similar colors (preserve edges)
                    val colorDiff = abs(pr - origR) + abs(pg - origG) + abs(pb - origB)
                    if (colorDiff < 80) { // Threshold for edge preservation
                        r += pr * weight
                        g += pg * weight
                        b += pb * weight
                        count += weight.toInt()
                    }
                }
            }
        }
        
        if (count == 0) return original
        
        val avgR = (r / count).toInt()
        val avgG = (g / count).toInt()
        val avgB = (b / count).toInt()
        
        // Much more subtle blending (max 40% smoothing)
        val blendFactor = intensity * 0.4f
        val finalR = (origR * (1 - blendFactor) + avgR * blendFactor).toInt().coerceIn(0, 255)
        val finalG = (origG * (1 - blendFactor) + avgG * blendFactor).toInt().coerceIn(0, 255)
        val finalB = (origB * (1 - blendFactor) + avgB * blendFactor).toInt().coerceIn(0, 255)
        
        return (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
    }
    
    private fun applySimpleBilateralFilter(pixels: IntArray, width: Int, height: Int, intensity: Float): IntArray {
        val result = IntArray(pixels.size)
        val radius = (3 * intensity).toInt().coerceAtLeast(1)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                result[y * width + x] = smoothPixel(pixels, width, height, x, y, intensity)
            }
        }
        
        return result
    }
    
    /**
     * Brighten specific region (for eye enhancement) with soft edges
     */
    private fun brightenRegion(bitmap: Bitmap, region: Rect, intensity: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(bitmap.width * bitmap.height)
        result.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val brightnessBoost = intensity * 0.25f // More subtle: up to 25% increase
        val centerX = (region.left + region.right) / 2f
        val centerY = (region.top + region.bottom) / 2f
        val radiusX = region.width() / 2f
        val radiusY = region.height() / 2f
        
        for (y in region.top until region.bottom) {
            for (x in region.left until region.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val idx = y * bitmap.width + x
                    val pixel = pixels[idx]
                    
                    // Calculate distance from center for soft falloff
                    val dx = (x - centerX) / radiusX
                    val dy = (y - centerY) / radiusY
                    val distance = sqrt(dx * dx + dy * dy)
                    val falloff = (1f - distance.coerceIn(0f, 1f)) // Smooth gradient
                    
                    val r = ((pixel shr 16) and 0xFF)
                    val g = ((pixel shr 8) and 0xFF)
                    val b = (pixel and 0xFF)
                    
                    // Apply brightness with falloff
                    val boost = 1f + (brightnessBoost * falloff)
                    val newR = (r * boost).toInt().coerceIn(0, 255)
                    val newG = (g * boost).toInt().coerceIn(0, 255)
                    val newB = (b * boost).toInt().coerceIn(0, 255)
                    
                    pixels[idx] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                }
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    /**
     * Apply blur to specific region with soft edges
     */
    private fun applyBlurToRegion(bitmap: Bitmap, region: Rect, intensity: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(bitmap.width * bitmap.height)
        result.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val blurRadius = (8 * intensity).toInt().coerceAtLeast(1)
        val centerX = (region.left + region.right) / 2f
        val centerY = (region.top + region.bottom) / 2f
        val radiusX = region.width() / 2f
        val radiusY = region.height() / 2f
        
        // Create blurred version of the region
        val blurredPixels = IntArray(bitmap.width * bitmap.height)
        for (y in region.top until region.bottom) {
            for (x in region.left until region.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val idx = y * bitmap.width + x
                    blurredPixels[idx] = blurPixel(pixels, bitmap.width, bitmap.height, x, y, blurRadius)
                }
            }
        }
        
        // Blend with soft edges
        for (y in region.top until region.bottom) {
            for (x in region.left until region.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val idx = y * bitmap.width + x
                    
                    // Calculate distance from center for soft falloff
                    val dx = (x - centerX) / radiusX
                    val dy = (y - centerY) / radiusY
                    val distance = sqrt(dx * dx + dy * dy)
                    val blendFactor = (1f - distance.coerceIn(0f, 1f)) // Smooth gradient
                    
                    val original = pixels[idx]
                    val blurred = blurredPixels[idx]
                    
                    val origR = (original shr 16) and 0xFF
                    val origG = (original shr 8) and 0xFF
                    val origB = original and 0xFF
                    
                    val blurR = (blurred shr 16) and 0xFF
                    val blurG = (blurred shr 8) and 0xFF
                    val blurB = blurred and 0xFF
                    
                    // Blend based on distance from center
                    val finalR = (origR * (1 - blendFactor) + blurR * blendFactor).toInt()
                    val finalG = (origG * (1 - blendFactor) + blurG * blendFactor).toInt()
                    val finalB = (origB * (1 - blendFactor) + blurB * blendFactor).toInt()
                    
                    pixels[idx] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                }
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    private fun blurPixel(pixels: IntArray, width: Int, height: Int, x: Int, y: Int, radius: Int): Int {
        var r = 0f
        var g = 0f
        var b = 0f
        var count = 0
        
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val nx = x + dx
                val ny = y + dy
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    val pixel = pixels[ny * width + nx]
                    r += ((pixel shr 16) and 0xFF)
                    g += ((pixel shr 8) and 0xFF)
                    b += (pixel and 0xFF)
                    count++
                }
            }
        }
        
        val avgR = (r / count).toInt().coerceIn(0, 255)
        val avgG = (g / count).toInt().coerceIn(0, 255)
        val avgB = (b / count).toInt().coerceIn(0, 255)
        
        return (0xFF shl 24) or (avgR shl 16) or (avgG shl 8) or avgB
    }
    
    /**
     * Remove blemishes and dark spots (acne, dark circles, etc.)
     */
    private fun removeBlemishes(bitmap: Bitmap, region: Rect, intensity: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(bitmap.width * bitmap.height)
        result.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        // Calculate average skin tone in the region
        var avgR = 0f
        var avgG = 0f
        var avgB = 0f
        var skinPixelCount = 0
        
        for (y in region.top until region.bottom) {
            for (x in region.left until region.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val pixel = pixels[y * bitmap.width + x]
                    if (isSkinTone(pixel)) {
                        avgR += ((pixel shr 16) and 0xFF)
                        avgG += ((pixel shr 8) and 0xFF)
                        avgB += (pixel and 0xFF)
                        skinPixelCount++
                    }
                }
            }
        }
        
        if (skinPixelCount == 0) return result
        
        avgR /= skinPixelCount
        avgG /= skinPixelCount
        avgB /= skinPixelCount
        
        // Find and correct dark spots (blemishes)
        for (y in region.top until region.bottom) {
            for (x in region.left until region.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val idx = y * bitmap.width + x
                    val pixel = pixels[idx]
                    
                    if (isSkinTone(pixel)) {
                        val r = ((pixel shr 16) and 0xFF)
                        val g = ((pixel shr 8) and 0xFF)
                        val b = (pixel and 0xFF)
                        
                        // Calculate luminance
                        val luminance = (0.299f * r + 0.587f * g + 0.114f * b)
                        val avgLuminance = (0.299f * avgR + 0.587f * avgG + 0.114f * avgB)
                        
                        // If pixel is significantly darker than average (blemish/dark spot)
                        val darknessDiff = avgLuminance - luminance
                        if (darknessDiff > 25) { // Increased threshold - only real blemishes
                            // Blend with surrounding pixels to hide blemish
                            val correctedPixel = getAverageSurroundingPixel(pixels, bitmap.width, bitmap.height, x, y, 3)
                            val corrR = (correctedPixel shr 16) and 0xFF
                            val corrG = (correctedPixel shr 8) and 0xFF
                            val corrB = correctedPixel and 0xFF
                            
                            // More subtle blending - only correct the blemish, not brighten everything
                            val blendFactor = (intensity * min(darknessDiff / 60f, 0.7f)).coerceIn(0f, 0.7f)
                            val finalR = (r * (1 - blendFactor) + corrR * blendFactor).toInt().coerceIn(0, 255)
                            val finalG = (g * (1 - blendFactor) + corrG * blendFactor).toInt().coerceIn(0, 255)
                            val finalB = (b * (1 - blendFactor) + corrB * blendFactor).toInt().coerceIn(0, 255)
                            
                            pixels[idx] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                        }
                    }
                }
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    /**
     * Brighten skin tone slightly for healthier appearance
     */
    private fun brightenSkinTone(bitmap: Bitmap, region: Rect, intensity: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(bitmap.width * bitmap.height)
        result.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        for (y in region.top until region.bottom) {
            for (x in region.left until region.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val idx = y * bitmap.width + x
                    val pixel = pixels[idx]
                    
                    if (isSkinTone(pixel)) {
                        val r = ((pixel shr 16) and 0xFF)
                        val g = ((pixel shr 8) and 0xFF)
                        val b = (pixel and 0xFF)
                        
                        // Slight brightness boost
                        val boost = 1f + (intensity * 0.1f)
                        val newR = (r * boost).toInt().coerceIn(0, 255)
                        val newG = (g * boost).toInt().coerceIn(0, 255)
                        val newB = (b * boost).toInt().coerceIn(0, 255)
                        
                        pixels[idx] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                    }
                }
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    /**
     * Get average color of surrounding pixels (for blemish correction)
     */
    private fun getAverageSurroundingPixel(pixels: IntArray, width: Int, height: Int, x: Int, y: Int, radius: Int): Int {
        var r = 0f
        var g = 0f
        var b = 0f
        var count = 0
        
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                if (dx == 0 && dy == 0) continue // Skip center pixel
                val nx = x + dx
                val ny = y + dy
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    val pixel = pixels[ny * width + nx]
                    if (isSkinTone(pixel)) {
                        r += ((pixel shr 16) and 0xFF)
                        g += ((pixel shr 8) and 0xFF)
                        b += (pixel and 0xFF)
                        count++
                    }
                }
            }
        }
        
        if (count == 0) return pixels[y * width + x]
        
        return (0xFF shl 24) or 
               (((r / count).toInt()) shl 16) or 
               (((g / count).toInt()) shl 8) or 
               ((b / count).toInt())
    }
    
    fun release() {
        faceDetector.close()
    }
}
