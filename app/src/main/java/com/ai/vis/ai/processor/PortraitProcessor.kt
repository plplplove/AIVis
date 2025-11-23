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
        
        // More strict skin tone detection to exclude hair
        // Skin has specific RGB ratios and ranges
        
        // Rule 1: Basic skin tone range (medium to dark skin)
        val rule1 = r > 95 && g > 40 && b > 20 &&
                    r > g && r > b &&
                    abs(r - g) > 15 && abs(r - g) < 80 && // Not too different
                    r - g > 15 && r - g < 80 && // R should be moderately higher than G
                    g > b // G should be higher than B
        
        // Rule 2: Lighter skin tones (but not hair)
        val rule2 = r > 200 && g > 180 && b > 150 &&
                    r > g && g > b && // Proper ordering
                    abs(r - g) < 25 && // Close values
                    abs(g - b) > 10 && abs(g - b) < 50 && // G and B not too close
                    r - b > 20 && r - b < 80 // R significantly higher than B
        
        // Rule 3: Exclude very bright pixels (likely background or highlights)
        val tooBright = r > 250 && g > 250 && b > 250
        
        // Rule 4: Exclude very dark pixels (likely hair or shadows)
        val tooDark = r < 50 || g < 40 || b < 30
        
        // Rule 5: Exclude hair-like colors (brown, black, blonde)
        val isHairLike = (r < 100 && g < 100 && b < 100) || // Dark hair
                         (abs(r - g) < 10 && abs(g - b) < 10 && r < 150) || // Brown/grey hair
                         (r > 200 && g > 180 && b < 150 && abs(r - g) < 15) // Blonde hair (yellowish)
        
        return (rule1 || rule2) && !tooBright && !tooDark && !isHairLike
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
        
        // Expand face region slightly to cover full face (including chin/mouth)
        // but use feathering to avoid harsh edges on neck
        val expandedRect = Rect(
            max(0, faceRect.left - (faceRect.width() * 0.05f).toInt()),
            faceRect.top,
            min(bitmap.width, faceRect.right + (faceRect.width() * 0.05f).toInt()),
            min(bitmap.height, faceRect.bottom + (faceRect.height() * 0.05f).toInt())
        )
        
        // Step 1: Remove blemishes and dark spots
        result = removeBlemishes(result, expandedRect, intensity)
        
        // Step 2: Apply skin smoothing to face region
        result = applySkinSmoothingToRegion(result, expandedRect, intensity)
        
        // Step 3: Add subtle makeup (blush, slight contrast boost)
        result = applySubtleMakeup(result, faceRect, intensity)
        
        Log.d(TAG, "Beauty mode applied successfully")
        result
    }
    
    /**
     * Remove dark circles under eyes: brighten and blur under-eye area
     */
    suspend fun enhanceEyes(bitmap: Bitmap, intensity: Float): Bitmap = withContext(Dispatchers.IO) {
        Log.d(TAG, "Removing dark circles with intensity: $intensity")
        
        val faceRect = detectFace(bitmap)
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        if (faceRect == null) {
            Log.w(TAG, "No face detected, skipping dark circle removal")
            return@withContext result
        }
        
        // Under-eye regions (bags/dark circles are BELOW the eyes)
        // Left under-eye area
        val leftUnderEyeRegion = Rect(
            faceRect.left + (faceRect.width() * 0.25f).toInt(),
            faceRect.top + (faceRect.height() * 0.40f).toInt(), // Below eye
            faceRect.left + (faceRect.width() * 0.45f).toInt(),
            faceRect.top + (faceRect.height() * 0.55f).toInt()
        )
        
        // Right under-eye area
        val rightUnderEyeRegion = Rect(
            faceRect.left + (faceRect.width() * 0.55f).toInt(),
            faceRect.top + (faceRect.height() * 0.40f).toInt(), // Below eye
            faceRect.left + (faceRect.width() * 0.75f).toInt(),
            faceRect.top + (faceRect.height() * 0.55f).toInt()
        )
        
        // Remove dark circles: brighten + blur
        var enhanced = removeDarkCircles(result, leftUnderEyeRegion, intensity)
        enhanced = removeDarkCircles(enhanced, rightUnderEyeRegion, intensity)
        
        Log.d(TAG, "Dark circles removed successfully")
        enhanced
    }
    
    /**
     * Blur skin only (not eyes, lips, eyebrows)
     */
    suspend fun blurFace(bitmap: Bitmap, intensity: Float): Bitmap = withContext(Dispatchers.IO) {
        Log.d(TAG, "Blurring skin with intensity: $intensity")
        
        val faceRect = detectFace(bitmap)
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        if (faceRect == null) {
            Log.w(TAG, "No face detected, skipping skin blur")
            return@withContext result
        }
        
        // Blur only skin pixels in face region
        val blurred = blurSkinOnly(result, faceRect, intensity)
        
        Log.d(TAG, "Skin blur applied successfully")
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
        
        // Calculate center and size for feathering
        val centerX = (region.left + region.right) / 2f
        val centerY = (region.top + region.bottom) / 2f
        val radiusX = region.width() / 2f
        val radiusY = region.height() / 2f
        
        // Apply smoothing only to skin pixels in face region with feathering
        for (y in region.top until region.bottom) {
            for (x in region.left until region.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val idx = y * bitmap.width + x
                    val pixel = pixels[idx]
                    
                    // Only smooth skin-tone pixels
                    if (isSkinTone(pixel)) {
                        // Calculate distance from center for feathering
                        val dx = (x - centerX) / radiusX
                        val dy = (y - centerY) / radiusY
                        val distance = sqrt(dx * dx + dy * dy)
                        
                        // Feather effect: full intensity in center, fades at edges
                        val feather = (1f - distance.coerceIn(0f, 1f))
                        val adjustedIntensity = intensity * feather
                        
                        if (adjustedIntensity > 0.1f) { // Only apply if intensity is meaningful
                            val smoothed = smoothPixel(pixels, bitmap.width, bitmap.height, x, y, adjustedIntensity)
                            // Blend original and smoothed based on feather
                            pixels[idx] = blendPixels(pixel, smoothed, feather)
                        }
                    }
                }
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    private fun blendPixels(pixel1: Int, pixel2: Int, ratio: Float): Int {
        val r1 = (pixel1 shr 16) and 0xFF
        val g1 = (pixel1 shr 8) and 0xFF
        val b1 = pixel1 and 0xFF
        
        val r2 = (pixel2 shr 16) and 0xFF
        val g2 = (pixel2 shr 8) and 0xFF
        val b2 = pixel2 and 0xFF
        
        val r = (r1 * (1 - ratio) + r2 * ratio).toInt().coerceIn(0, 255)
        val g = (g1 * (1 - ratio) + g2 * ratio).toInt().coerceIn(0, 255)
        val b = (b1 * (1 - ratio) + b2 * ratio).toInt().coerceIn(0, 255)
        
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
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
        
        // Calculate center for feathering
        val centerX = (region.left + region.right) / 2f
        val centerY = (region.top + region.bottom) / 2f
        val radiusX = region.width() / 2f
        val radiusY = region.height() / 2f
        
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
        
        // Find and correct dark spots (blemishes) with feathering
        for (y in region.top until region.bottom) {
            for (x in region.left until region.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val idx = y * bitmap.width + x
                    val pixel = pixels[idx]
                    
                    if (isSkinTone(pixel)) {
                        // Calculate feathering
                        val dx = (x - centerX) / radiusX
                        val dy = (y - centerY) / radiusY
                        val distance = sqrt(dx * dx + dy * dy)
                        val feather = (1f - distance.coerceIn(0f, 1f))
                        
                        val r = ((pixel shr 16) and 0xFF)
                        val g = ((pixel shr 8) and 0xFF)
                        val b = (pixel and 0xFF)
                        
                        // Calculate luminance
                        val luminance = (0.299f * r + 0.587f * g + 0.114f * b)
                        val avgLuminance = (0.299f * avgR + 0.587f * avgG + 0.114f * avgB)
                        
                        // If pixel is significantly darker than average (blemish/dark spot)
                        val darknessDiff = avgLuminance - luminance
                        if (darknessDiff > 20) { // Lower threshold - catch more blemishes
                            // Blend with surrounding pixels to hide blemish
                            val correctedPixel = getAverageSurroundingPixel(pixels, bitmap.width, bitmap.height, x, y, 3)
                            val corrR = (correctedPixel shr 16) and 0xFF
                            val corrG = (correctedPixel shr 8) and 0xFF
                            val corrB = correctedPixel and 0xFF
                            
                            // Apply feathering to blend factor
                            // Stronger correction for blemishes
                            val baseBlend = (intensity * min(darknessDiff / 60f, 0.6f)).coerceIn(0f, 0.6f)
                            val blendFactor = baseBlend * feather
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
        
        val centerPixel = pixels[y * width + x]
        val centerLuminance = (0.299f * ((centerPixel shr 16) and 0xFF) + 
                               0.587f * ((centerPixel shr 8) and 0xFF) + 
                               0.114f * (centerPixel and 0xFF))
        
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                if (dx == 0 && dy == 0) continue // Skip center pixel
                val nx = x + dx
                val ny = y + dy
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    val pixel = pixels[ny * width + nx]
                    if (isSkinTone(pixel)) {
                        // Only use pixels that are NOT darker (avoid spreading blemish)
                        val pixelLuminance = (0.299f * ((pixel shr 16) and 0xFF) + 
                                             0.587f * ((pixel shr 8) and 0xFF) + 
                                             0.114f * (pixel and 0xFF))
                        
                        if (pixelLuminance >= centerLuminance - 10) { // Only use similar or brighter pixels
                            r += ((pixel shr 16) and 0xFF)
                            g += ((pixel shr 8) and 0xFF)
                            b += (pixel and 0xFF)
                            count++
                        }
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
    
    /**
     * Apply subtle makeup: slight blush on cheeks, enhance lips slightly
     */
    private fun applySubtleMakeup(bitmap: Bitmap, faceRect: Rect, intensity: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(bitmap.width * bitmap.height)
        result.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val centerX = (faceRect.left + faceRect.right) / 2f
        val centerY = (faceRect.top + faceRect.bottom) / 2f
        
        // Cheek regions (for blush)
        val leftCheekX = faceRect.left + (faceRect.width() * 0.25f)
        val rightCheekX = faceRect.left + (faceRect.width() * 0.75f)
        val cheekY = faceRect.top + (faceRect.height() * 0.55f)
        val cheekRadius = faceRect.width() * 0.15f
        
        // Lip region (for subtle enhancement)
        val lipY = faceRect.top + (faceRect.height() * 0.75f)
        val lipRadius = faceRect.width() * 0.12f
        
        for (y in faceRect.top until faceRect.bottom) {
            for (x in faceRect.left until faceRect.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val idx = y * bitmap.width + x
                    val pixel = pixels[idx]
                    
                    if (isSkinTone(pixel)) {
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        
                        // Check if in cheek area (add pink/rosy tint)
                        val distToLeftCheek = sqrt((x - leftCheekX) * (x - leftCheekX) + (y - cheekY) * (y - cheekY))
                        val distToRightCheek = sqrt((x - rightCheekX) * (x - rightCheekX) + (y - cheekY) * (y - cheekY))
                        
                        if (distToLeftCheek < cheekRadius || distToRightCheek < cheekRadius) {
                            val dist = min(distToLeftCheek, distToRightCheek)
                            val blushStrength = (1f - (dist / cheekRadius)) * intensity * 0.15f // Very subtle
                            
                            // Add rosy tint
                            val newR = (r + 15 * blushStrength).toInt().coerceIn(0, 255)
                            val newG = (g - 5 * blushStrength).toInt().coerceIn(0, 255)
                            val newB = (b - 5 * blushStrength).toInt().coerceIn(0, 255)
                            
                            pixels[idx] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                        }
                    }
                    
                    // Check if in lip area (enhance color slightly)
                    val distToLips = sqrt((x - centerX) * (x - centerX) + (y - lipY) * (y - lipY))
                    if (distToLips < lipRadius) {
                        val lipStrength = (1f - (distToLips / lipRadius)) * intensity * 0.1f // Very subtle
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        
                        // Enhance red in lips
                        val newR = (r + 10 * lipStrength).toInt().coerceIn(0, 255)
                        val newG = (g * (1f - 0.05f * lipStrength)).toInt().coerceIn(0, 255)
                        val newB = (b * (1f - 0.05f * lipStrength)).toInt().coerceIn(0, 255)
                        
                        pixels[idx] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                    }
                }
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    /**
     * Remove dark circles: brighten and blur under-eye area
     */
    private fun removeDarkCircles(bitmap: Bitmap, region: Rect, intensity: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(bitmap.width * bitmap.height)
        result.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val centerX = (region.left + region.right) / 2f
        val centerY = (region.top + region.bottom) / 2f
        val radiusX = region.width() / 2f
        val radiusY = region.height() / 2f
        
        // First pass: brighten dark areas
        for (y in region.top until region.bottom) {
            for (x in region.left until region.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val idx = y * bitmap.width + x
                    val pixel = pixels[idx]
                    
                    // Calculate distance for feathering
                    val dx = (x - centerX) / radiusX
                    val dy = (y - centerY) / radiusY
                    val distance = sqrt(dx * dx + dy * dy)
                    val feather = (1f - distance.coerceIn(0f, 1f))
                    
                    if (feather > 0.1f) {
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        
                        // Brighten dark pixels (dark circles)
                        val brightenAmount = intensity * feather * 0.2f // Subtle brightening
                        val newR = (r * (1f + brightenAmount)).toInt().coerceIn(0, 255)
                        val newG = (g * (1f + brightenAmount)).toInt().coerceIn(0, 255)
                        val newB = (b * (1f + brightenAmount)).toInt().coerceIn(0, 255)
                        
                        pixels[idx] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                    }
                }
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        // Second pass: apply subtle blur to smooth the area
        return applyBlurToRegion(result, region, intensity * 0.5f)
    }
    
    /**
     * Blur only skin pixels in face region (skip eyes, lips, eyebrows)
     */
    private fun blurSkinOnly(bitmap: Bitmap, faceRect: Rect, intensity: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(bitmap.width * bitmap.height)
        result.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val centerX = (faceRect.left + faceRect.right) / 2f
        val centerY = (faceRect.top + faceRect.bottom) / 2f
        val radiusX = faceRect.width() / 2f
        val radiusY = faceRect.height() / 2f
        
        // Define regions to EXCLUDE (eyes, eyebrows, lips)
        // Eyes + eyebrows
        val eyeTop = faceRect.top + (faceRect.height() * 0.25f).toInt()
        val eyeBottom = faceRect.top + (faceRect.height() * 0.50f).toInt()
        val eyeLeft = faceRect.left + (faceRect.width() * 0.20f).toInt()
        val eyeRight = faceRect.left + (faceRect.width() * 0.80f).toInt()
        
        // Lips
        val lipTop = faceRect.top + (faceRect.height() * 0.70f).toInt()
        val lipBottom = faceRect.top + (faceRect.height() * 0.85f).toInt()
        val lipLeft = faceRect.left + (faceRect.width() * 0.35f).toInt()
        val lipRight = faceRect.left + (faceRect.width() * 0.65f).toInt()
        
        // Apply blur only to skin pixels, excluding eyes and lips
        for (y in faceRect.top until faceRect.bottom) {
            for (x in faceRect.left until faceRect.right) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    // Skip eye region (including eyebrows)
                    val inEyeRegion = y >= eyeTop && y <= eyeBottom && x >= eyeLeft && x <= eyeRight
                    
                    // Skip lip region
                    val inLipRegion = y >= lipTop && y <= lipBottom && x >= lipLeft && x <= lipRight
                    
                    if (inEyeRegion || inLipRegion) continue
                    
                    val idx = y * bitmap.width + x
                    val pixel = pixels[idx]
                    
                    // Only blur skin pixels
                    if (isSkinTone(pixel)) {
                        // Calculate feathering
                        val dx = (x - centerX) / radiusX
                        val dy = (y - centerY) / radiusY
                        val distance = sqrt(dx * dx + dy * dy)
                        val feather = (1f - distance.coerceIn(0f, 1f))
                        
                        if (feather > 0.1f) {
                            val blurred = blurPixel(pixels, bitmap.width, bitmap.height, x, y, intensity)
                            // Stronger blend for more visible blur
                            pixels[idx] = blendPixels(pixel, blurred, 0.7f * intensity)
                        }
                    }
                }
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    /**
     * Blur a single pixel by averaging with neighbors
     */
    private fun blurPixel(pixels: IntArray, width: Int, height: Int, x: Int, y: Int, intensity: Float): Int {
        val radius = (3 * intensity).toInt().coerceAtLeast(1)
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
        
        if (count == 0) return pixels[y * width + x]
        
        return (0xFF shl 24) or 
               (((r / count).toInt().coerceIn(0, 255)) shl 16) or
               (((g / count).toInt().coerceIn(0, 255)) shl 8) or
               ((b / count).toInt().coerceIn(0, 255))
    }
    
    fun release() {
        faceDetector.close()
    }
}
