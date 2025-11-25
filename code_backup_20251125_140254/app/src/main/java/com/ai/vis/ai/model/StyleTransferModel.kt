package com.ai.vis.ai.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ai.vis.domain.model.AIStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class StyleTransferModel(private val context: Context) {
    
    private var stylePredictInterpreter: Interpreter? = null
    private var styleTransferInterpreter: Interpreter? = null
    private var currentStyle: AIStyle = AIStyle.NONE
    private var cachedStyleBottleneck: FloatArray? = null
    
    private val styleImageSize = 256
    private val contentImageSize = 384
    
    suspend fun initialize(style: AIStyle) = withContext(Dispatchers.IO) {
        if (style == AIStyle.NONE) {
            cachedStyleBottleneck = null
            currentStyle = AIStyle.NONE
            return@withContext
        }
        
        if (currentStyle == style && cachedStyleBottleneck != null) {
            return@withContext
        }
        
        currentStyle = style
        
        try {
            if (stylePredictInterpreter == null) {
                android.util.Log.d("StyleTransfer", "Loading style_predict model...")
                val predictModelPath = try {
                    context.assets.openFd("models/style_predict_v2.tflite")
                    android.util.Log.d("StyleTransfer", "Using v2 prediction model")
                    "models/style_predict_v2.tflite"
                } catch (e: Exception) {
                    android.util.Log.d("StyleTransfer", "v2 not found, using v1 prediction model")
                    "models/style_predict.tflite"
                }
                val predictModel = loadModelFromAssets(predictModelPath)
                val predictOptions = Interpreter.Options().apply {
                    setNumThreads(Runtime.getRuntime().availableProcessors())
                    setUseXNNPACK(true)
                }
                stylePredictInterpreter = Interpreter(predictModel, predictOptions)
                android.util.Log.d("StyleTransfer", "style_predict model loaded successfully")
                
                stylePredictInterpreter?.let { interpreter ->
                    android.util.Log.d("StyleTransfer", "Predict input count: ${interpreter.inputTensorCount}")
                    android.util.Log.d("StyleTransfer", "Predict output count: ${interpreter.outputTensorCount}")
                    android.util.Log.d("StyleTransfer", "Predict input shape: ${interpreter.getInputTensor(0).shape().contentToString()}")
                    android.util.Log.d("StyleTransfer", "Predict output shape: ${interpreter.getOutputTensor(0).shape().contentToString()}")
                }
            }
            
            if (styleTransferInterpreter == null) {
                android.util.Log.d("StyleTransfer", "Loading style_transfer model...")
                val transferModelPath = try {
                    context.assets.openFd("models/style_transfer_v2.tflite")
                    android.util.Log.d("StyleTransfer", "Using v2 transfer model")
                    "models/style_transfer_v2.tflite"
                } catch (e: Exception) {
                    android.util.Log.d("StyleTransfer", "v2 not found, using v1 transfer model")
                    "models/style_transfer.tflite"
                }
                val transferModel = loadModelFromAssets(transferModelPath)
                val transferOptions = Interpreter.Options().apply {
                    setNumThreads(Runtime.getRuntime().availableProcessors())
                    setUseXNNPACK(true)
                }
                styleTransferInterpreter = Interpreter(transferModel, transferOptions)
                android.util.Log.d("StyleTransfer", "style_transfer model loaded successfully")
                
                styleTransferInterpreter?.let { interpreter ->
                    android.util.Log.d("StyleTransfer", "Transfer input count: ${interpreter.inputTensorCount}")
                    android.util.Log.d("StyleTransfer", "Transfer output count: ${interpreter.outputTensorCount}")
                    for (i in 0 until interpreter.inputTensorCount) {
                        android.util.Log.d("StyleTransfer", "Transfer input[$i] shape: ${interpreter.getInputTensor(i).shape().contentToString()}")
                    }
                    android.util.Log.d("StyleTransfer", "Transfer output shape: ${interpreter.getOutputTensor(0).shape().contentToString()}")
                }
            }
            
            style.styleImagePath?.let { path ->
                android.util.Log.d("StyleTransfer", "Loading style image from: $path")
                val styleImage = loadStyleImageFromAssets(path)
                android.util.Log.d("StyleTransfer", "Style image size: ${styleImage.width}x${styleImage.height}")
                cachedStyleBottleneck = predictStyleBottleneck(styleImage)
                android.util.Log.d("StyleTransfer", "Style bottleneck computed, size: ${cachedStyleBottleneck?.size}")
                styleImage.recycle()
            }
        } catch (e: Exception) {
            android.util.Log.e("StyleTransfer", "Error initializing model", e)
            cachedStyleBottleneck = null
        }
    }
    
    private fun loadModelFromAssets(fileName: String): MappedByteBuffer {
        val fd = context.assets.openFd(fileName)
        FileInputStream(fd.fileDescriptor).use { input ->
            val channel = input.channel
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                fd.startOffset,
                fd.declaredLength
            )
        }
    }
    
    private fun loadStyleImageFromAssets(path: String): Bitmap {
        context.assets.open(path).use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)!!
            return createHighQualityScaledBitmap(bitmap, styleImageSize, styleImageSize).also {
                if (it != bitmap) bitmap.recycle()
            }
        }
    }
    
    private fun calculateOptimalSize(width: Int, height: Int, maxSize: Int): Int {
        // Avoid always forcing the image to the maximum model size.
        // If the image is already smaller than the model max, keep the original largest dimension
        // to prevent unnecessary upscaling which causes blurring and artifacts.
        val maxDim = maxOf(width, height)
        return if (maxDim <= maxSize) maxDim else maxSize
    }
    
    private fun applyPostProcessing(bitmap: Bitmap): Bitmap {
        var result = bitmap

        // Apply a gentle unsharp mask first (better than naive convolution for avoiding haloing),
        // then moderate contrast and a small saturation boost.
        result = enhanceSharpness(result, 0.25f)
        result = enhanceContrast(result, 0.12f)
        result = enhanceSaturation(result, 0.08f)

        return result
    }
    
    private fun enhanceContrast(bitmap: Bitmap, amount: Float): Bitmap {
        val contrast = 1f + amount
        val translate = (-.5f * contrast + .5f) * 255f
        
        val colorMatrix = android.graphics.ColorMatrix().apply {
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        if (result != bitmap) bitmap.recycle()
        return result
    }
    
    private fun enhanceSharpness(bitmap: Bitmap, amount: Float): Bitmap {
        if (amount <= 0f) return bitmap

        // Unsharp mask implementation (fast approximation):
        // 1) Create a blurred version by scaling down then back up (fast box-like blur)
        // 2) new = orig + amount * (orig - blurred)

        val smallW = (bitmap.width / 2).coerceAtLeast(1)
        val smallH = (bitmap.height / 2).coerceAtLeast(1)

        val down = createHighQualityScaledBitmap(bitmap, smallW, smallH)
        val blurred = createHighQualityScaledBitmap(down, bitmap.width, bitmap.height)
        if (down != bitmap) down.recycle()

        val origPixels = IntArray(bitmap.width * bitmap.height)
        val blurPixels = IntArray(origPixels.size)
        bitmap.getPixels(origPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        blurred.getPixels(blurPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val outPixels = IntArray(origPixels.size)
        val factor = amount.coerceIn(0f, 2f)

        for (i in origPixels.indices) {
            val p = origPixels[i]
            val b = blurPixels[i]

            val orr = (p shr 16) and 0xFF
            val ogg = (p shr 8) and 0xFF
            val obb = p and 0xFF

            val brr = (b shr 16) and 0xFF
            val bgg = (b shr 8) and 0xFF
            val bbb = b and 0xFF

            val r = (orr + (factor * (orr - brr))).toInt().coerceIn(0, 255)
            val g = (ogg + (factor * (ogg - bgg))).toInt().coerceIn(0, 255)
            val bb = (obb + (factor * (obb - bbb))).toInt().coerceIn(0, 255)

            outPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bb
        }

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        if (blurred != bitmap) blurred.recycle()
        if (result != bitmap) bitmap.recycle()
        return result
    }
    
    private fun enhanceSaturation(bitmap: Bitmap, amount: Float): Bitmap {
        val saturation = 1f + amount
        val colorMatrix = android.graphics.ColorMatrix().apply {
            setSaturation(saturation)
        }
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        if (result != bitmap) bitmap.recycle()
        return result
    }
    
    private fun createHighQualityScaledBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
        if (source.width == width && source.height == height) {
            return source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        }
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
        
        val srcRect = android.graphics.Rect(0, 0, source.width, source.height)
        val dstRect = android.graphics.Rect(0, 0, width, height)
        
        canvas.drawBitmap(source, srcRect, dstRect, paint)
        return result
    }
    
    private fun predictStyleBottleneck(styleImage: Bitmap): FloatArray {
        android.util.Log.d("StyleTransfer", "predictStyleBottleneck: Converting bitmap to buffer...")
        val inputBuffer = bitmapToByteBuffer(styleImage, styleImageSize)
        android.util.Log.d("StyleTransfer", "predictStyleBottleneck: Input buffer size: ${inputBuffer.capacity()}")
        
        val outputArray = Array(1) { Array(1) { Array(1) { FloatArray(100) } } }
        
        android.util.Log.d("StyleTransfer", "predictStyleBottleneck: Running inference...")
        stylePredictInterpreter?.run(inputBuffer, outputArray)
        android.util.Log.d("StyleTransfer", "predictStyleBottleneck: Inference complete")
        
        val result = outputArray[0][0][0]
        android.util.Log.d("StyleTransfer", "predictStyleBottleneck: Result array size: ${result.size}, first values: [${result.take(5).joinToString()}]")
        return result
    }
    
    suspend fun applyStyle(bitmap: Bitmap): Bitmap = withContext(Dispatchers.IO) {
        android.util.Log.d("StyleTransfer", "applyStyle: Starting for style $currentStyle")
        val styleBottleneck = cachedStyleBottleneck
        val transferInterpreter = styleTransferInterpreter
        
        if (styleBottleneck == null || transferInterpreter == null) {
            android.util.Log.w("StyleTransfer", "applyStyle: Using fallback - bottleneck=${styleBottleneck != null}, interpreter=${transferInterpreter != null}")
            return@withContext applyFallbackStyle(bitmap, currentStyle)
        }
        
        try {
            // If image is large, perform tiled inference with overlap and blending
            val maxDim = maxOf(bitmap.width, bitmap.height)
            val tileThreshold = contentImageSize * 2 // if larger than two model sizes, use tiling

            // Pre-denoise to reduce noise amplification
            val preDenoised = if (maxDim > contentImageSize) applyFastEdgePreserveDenoise(bitmap, 6, 0.9f) else bitmap

            val styled = if (maxDim > tileThreshold) {
                android.util.Log.d("StyleTransfer", "applyStyle: Using tiled inference for large image ${bitmap.width}x${bitmap.height}")
                applyStyleTiled(preDenoised, styleBottleneck)
            } else {
                // small/medium image: run single-shot
                val targetSize = calculateOptimalSize(preDenoised.width, preDenoised.height, contentImageSize)
                android.util.Log.d("StyleTransfer", "applyStyle: Scaling bitmap from ${preDenoised.width}x${preDenoised.height} to ${targetSize}x${targetSize}")

                val scaledBitmap = if (preDenoised.width == targetSize && preDenoised.height == targetSize) {
                    preDenoised
                } else {
                    createHighQualityScaledBitmap(preDenoised, targetSize, targetSize)
                }

                val outputBitmap = runStyleTransferOnBitmap(scaledBitmap, styleBottleneck)

                val enhancedBitmap = applyPostProcessing(outputBitmap)

                val resultBitmap = if (enhancedBitmap.width != bitmap.width || enhancedBitmap.height != bitmap.height) {
                    createHighQualityScaledBitmap(
                        enhancedBitmap,
                        bitmap.width,
                        bitmap.height
                    )
                } else {
                    enhancedBitmap
                }

                if (scaledBitmap != preDenoised) scaledBitmap.recycle()
                if (enhancedBitmap != outputBitmap) outputBitmap.recycle()
                resultBitmap
            }

            // Post-denoise to reduce any remaining tile seams/noise before returning
            val post = if (styled != bitmap) applyFastEdgePreserveDenoise(styled, 4, 0.6f) else styled

            if (preDenoised != bitmap && preDenoised != styled) preDenoised.recycle()

            android.util.Log.d("StyleTransfer", "applyStyle: SUCCESS!")
            post
        } catch (e: Exception) {
            android.util.Log.e("StyleTransfer", "applyStyle: ERROR - falling back", e)
            applyFallbackStyle(bitmap, currentStyle)
        }
    }

    private fun runStyleTransferOnBitmap(scaledBitmap: Bitmap, styleBottleneck: FloatArray): Bitmap {
        val transferInterpreter = styleTransferInterpreter ?: throw IllegalStateException("transfer interpreter null")
        val targetSize = scaledBitmap.width

        val contentBuffer = bitmapToByteBuffer(scaledBitmap, targetSize)
        val styleBuffer = styleBottleneckToByteBuffer(styleBottleneck)

        val inputs = arrayOf(contentBuffer, styleBuffer)
        val outputMap = HashMap<Int, Any>()
        val outputArray = Array(1) { Array(targetSize) { Array(targetSize) { FloatArray(3) } } }
        outputMap[0] = outputArray

        transferInterpreter.runForMultipleInputsOutputs(inputs, outputMap)

        return floatArrayToBitmap(outputArray[0], targetSize, targetSize)
    }

    private fun applyStyleTiled(bitmap: Bitmap, styleBottleneck: FloatArray): Bitmap {
        val tileSize = contentImageSize
        val overlap = (tileSize * 0.25).toInt().coerceAtLeast(16)

        val width = bitmap.width
        val height = bitmap.height

        val accumR = FloatArray(width * height)
        val accumG = FloatArray(width * height)
        val accumB = FloatArray(width * height)
        val accumW = FloatArray(width * height)

        var y = 0
        while (y < height) {
            var x = 0
            val tileH = if (y + tileSize >= height) height - y else tileSize
            while (x < width) {
                val tileW = if (x + tileSize >= width) width - x else tileSize

                val tileRect = android.graphics.Rect(x, y, x + tileW, y + tileH)
                val tileBitmap = Bitmap.createBitmap(bitmap, tileRect.left, tileRect.top, tileRect.width(), tileRect.height())

                // Scale tile to model input size
                val scaledTile = createHighQualityScaledBitmap(tileBitmap, tileSize, tileSize)
                val outTile = runStyleTransferOnBitmap(scaledTile, styleBottleneck)

                // Scale output back to tile size
                val backTile = createHighQualityScaledBitmap(outTile, tileW, tileH)

                // Blend into accumulators with triangular weight (feather)
                for (ty in 0 until tileH) {
                    val gy = tileRect.top + ty
                    for (tx in 0 until tileW) {
                        val gx = tileRect.left + tx
                        val globalIdx = gy * width + gx

                        val dx = if (tileW == 1) 0f else (tx.toFloat() / (tileW - 1))
                        val dy = if (tileH == 1) 0f else (ty.toFloat() / (tileH - 1))

                        // triangular weight: product of 1 - distance to edge
                        val wx = 1f - Math.abs(2f * dx - 1f)
                        val wy = 1f - Math.abs(2f * dy - 1f)
                        val weight = (wx * wy).coerceIn(0f, 1f)

                        val p = backTile.getPixel(tx, ty)
                        val r = ((p shr 16) and 0xFF).toFloat()
                        val g = ((p shr 8) and 0xFF).toFloat()
                        val b = (p and 0xFF).toFloat()

                        accumR[globalIdx] += r * weight
                        accumG[globalIdx] += g * weight
                        accumB[globalIdx] += b * weight
                        accumW[globalIdx] += weight
                    }
                }

                if (scaledTile != tileBitmap) scaledTile.recycle()
                if (outTile != scaledTile) outTile.recycle()
                if (backTile != outTile) backTile.recycle()
                if (tileBitmap != bitmap) tileBitmap.recycle()

                x += (tileSize - overlap)
            }
            y += (tileSize - overlap)
        }

        // Compose final image
        val result = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (i in 0 until width * height) {
            val w = accumW[i]
            if (w <= 0f) {
                pixels[i] = 0xFF000000.toInt()
            } else {
                val rr = (accumR[i] / w + 0.5f).toInt().coerceIn(0, 255)
                val gg = (accumG[i] / w + 0.5f).toInt().coerceIn(0, 255)
                val bb = (accumB[i] / w + 0.5f).toInt().coerceIn(0, 255)
                pixels[i] = (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
            }
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Fast edge-preserving denoise: creates blurred base by downscaling/upscaling and blends
     * with original using a gradient-based edge mask so edges are preserved while flat areas are smoothed.
     * radius: controls blur amount (downscale factor)
     * strength: 0..1 how strong denoising is
     */
    private fun applyFastEdgePreserveDenoise(bitmap: Bitmap, radius: Int = 4, strength: Float = 0.7f): Bitmap {
        if (radius <= 0 || strength <= 0f) return bitmap

        val factor = radius.coerceAtLeast(2)
        val downW = (bitmap.width / factor).coerceAtLeast(1)
        val downH = (bitmap.height / factor).coerceAtLeast(1)

        val down = createHighQualityScaledBitmap(bitmap, downW, downH)
        val blurred = createHighQualityScaledBitmap(down, bitmap.width, bitmap.height)
        if (down != bitmap) down.recycle()

        val width = bitmap.width
        val height = bitmap.height
        val origPixels = IntArray(width * height)
        val blurPixels = IntArray(width * height)
        bitmap.getPixels(origPixels, 0, width, 0, 0, width, height)
        blurred.getPixels(blurPixels, 0, width, 0, 0, width, height)

        // compute luminance and gradient magnitude via simple Sobel
        val lum = FloatArray(width * height)
        for (i in origPixels.indices) {
            val p = origPixels[i]
            val r = ((p shr 16) and 0xFF)
            val g = ((p shr 8) and 0xFF)
            val b = (p and 0xFF)
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        val grad = FloatArray(width * height)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val gx = -lum[idx - width - 1] - 2f * lum[idx - 1] - lum[idx + width - 1] + lum[idx - width + 1] + 2f * lum[idx + 1] + lum[idx + width + 1]
                val gy = -lum[idx - width - 1] - 2f * lum[idx - width] - lum[idx - width + 1] + lum[idx + width - 1] + 2f * lum[idx + width] + lum[idx + width + 1]
                grad[idx] = kotlin.math.sqrt(gx * gx + gy * gy)
            }
        }

        // find max gradient for normalization
        var maxGrad = 1f
        for (v in grad) if (v > maxGrad) maxGrad = v

        val outPixels = IntArray(origPixels.size)
        val keepFactor = strength.coerceIn(0f, 1f)
        for (i in origPixels.indices) {
            val g = (grad[i] / maxGrad).coerceIn(0f, 1f)
            // mask: near edges keep original (g ~ 1), in flat areas use blurred (g ~ 0)
            val mask = g

            val p = origPixels[i]
            val bp = blurPixels[i]

            val orr = (p shr 16) and 0xFF
            val ogg = (p shr 8) and 0xFF
            val obb = p and 0xFF

            val brr = (bp shr 16) and 0xFF
            val bgg = (bp shr 8) and 0xFF
            val bbb = bp and 0xFF

            // first blend blurred and original based on mask, then interpolate by keepFactor
            val blendedR = (orr * mask + brr * (1f - mask)).toInt()
            val blendedG = (ogg * mask + bgg * (1f - mask)).toInt()
            val blendedB = (obb * mask + bbb * (1f - mask)).toInt()

            val finalR = (orr * keepFactor + blendedR * (1f - keepFactor)).toInt().coerceIn(0, 255)
            val finalG = (ogg * keepFactor + blendedG * (1f - keepFactor)).toInt().coerceIn(0, 255)
            val finalB = (obb * keepFactor + blendedB * (1f - keepFactor)).toInt().coerceIn(0, 255)

            outPixels[i] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
        }

        val result = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)

        if (blurred != bitmap) blurred.recycle()
        return result
    }
    
    private fun styleBottleneckToByteBuffer(styleBottleneck: FloatArray): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * 1 * 1 * 100 * 4)
        buffer.order(ByteOrder.nativeOrder())
        
        for (value in styleBottleneck) {
            buffer.putFloat(value)
        }
        
        buffer.rewind()
        return buffer
    }
    
    private fun applyFallbackStyle(bitmap: Bitmap, style: AIStyle): Bitmap {
        if (style == AIStyle.NONE) return bitmap
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        when (style) {
            AIStyle.OIL_PAINTING -> {
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(
                    android.graphics.ColorMatrix().apply {
                        setSaturation(1.3f)
                    }
                )
            }
            AIStyle.WATERCOLOR -> {
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(
                    android.graphics.ColorMatrix(floatArrayOf(
                        0.9f, 0.1f, 0.1f, 0f, 10f,
                        0.1f, 0.9f, 0.1f, 0f, 10f,
                        0.1f, 0.1f, 0.9f, 0f, 10f,
                        0f, 0f, 0f, 0.9f, 0f
                    ))
                )
            }
            AIStyle.CARTOON -> {
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(
                    android.graphics.ColorMatrix().apply {
                        setSaturation(1.5f)
                    }
                )
            }
            AIStyle.PENCIL_SKETCH -> {
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(
                    android.graphics.ColorMatrix().apply {
                        setSaturation(0f)
                    }
                )
            }
            AIStyle.VAN_GOGH -> {
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(
                    android.graphics.ColorMatrix(floatArrayOf(
                        1.2f, 0f, 0f, 0f, 20f,
                        0f, 1.1f, 0f, 0f, 10f,
                        0f, 0f, 1.3f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                )
            }
            AIStyle.POP_ART -> {
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(
                    android.graphics.ColorMatrix().apply {
                        setSaturation(2f)
                    }
                )
            }
            AIStyle.IMPRESSIONISM -> {
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(
                    android.graphics.ColorMatrix(floatArrayOf(
                        1.1f, 0.1f, 0f, 0f, 15f,
                        0.1f, 1.1f, 0.1f, 0f, 15f,
                        0f, 0.1f, 1.1f, 0f, 15f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                )
            }
            AIStyle.NONE -> return bitmap
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    private fun bitmapToByteBuffer(bitmap: Bitmap, size: Int): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * size * size * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        
        byteBuffer.rewind()
        return byteBuffer
    }
    
    private fun floatArrayToBitmap(floatArray: Array<Array<FloatArray>>, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        
        var index = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (floatArray[y][x][0].coerceIn(0f, 1f) * 255f + 0.5f).toInt().coerceIn(0, 255)
                val g = (floatArray[y][x][1].coerceIn(0f, 1f) * 255f + 0.5f).toInt().coerceIn(0, 255)
                val b = (floatArray[y][x][2].coerceIn(0f, 1f) * 255f + 0.5f).toInt().coerceIn(0, 255)
                
                pixels[index++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
    
    fun release() {
        stylePredictInterpreter?.close()
        styleTransferInterpreter?.close()
        stylePredictInterpreter = null
        styleTransferInterpreter = null
        cachedStyleBottleneck = null
        currentStyle = AIStyle.NONE
    }
}
