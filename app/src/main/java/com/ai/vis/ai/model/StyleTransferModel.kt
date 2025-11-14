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
                val predictModel = loadModelFromAssets("models/style_predict.tflite")
                val predictOptions = Interpreter.Options().apply {
                    setNumThreads(4)
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
                val transferModel = loadModelFromAssets("models/style_transfer.tflite")
                val transferOptions = Interpreter.Options().apply {
                    setNumThreads(4)
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
            val bitmap = BitmapFactory.decodeStream(inputStream)
            return Bitmap.createScaledBitmap(bitmap, styleImageSize, styleImageSize, true)
        }
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
            android.util.Log.d("StyleTransfer", "applyStyle: Scaling bitmap from ${bitmap.width}x${bitmap.height} to ${contentImageSize}x${contentImageSize}")
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, contentImageSize, contentImageSize, true)
            
            android.util.Log.d("StyleTransfer", "applyStyle: Converting content to buffer...")
            val contentBuffer = bitmapToByteBuffer(scaledBitmap, contentImageSize)
            android.util.Log.d("StyleTransfer", "applyStyle: Content buffer size: ${contentBuffer.capacity()}")
            
            android.util.Log.d("StyleTransfer", "applyStyle: Converting style bottleneck to buffer...")
            val styleBuffer = styleBottleneckToByteBuffer(styleBottleneck)
            android.util.Log.d("StyleTransfer", "applyStyle: Style buffer size: ${styleBuffer.capacity()}")
            
            val inputs = arrayOf(contentBuffer, styleBuffer)
            val outputMap = HashMap<Int, Any>()
            val outputArray = Array(1) { Array(contentImageSize) { Array(contentImageSize) { FloatArray(3) } } }
            outputMap[0] = outputArray
            
            android.util.Log.d("StyleTransfer", "applyStyle: Running style transfer inference...")
            transferInterpreter.runForMultipleInputsOutputs(inputs, outputMap)
            android.util.Log.d("StyleTransfer", "applyStyle: Inference complete")
            
            android.util.Log.d("StyleTransfer", "applyStyle: Converting output to bitmap...")
            val outputBitmap = floatArrayToBitmap(outputArray[0], contentImageSize, contentImageSize)
            
            android.util.Log.d("StyleTransfer", "applyStyle: Scaling result to ${bitmap.width}x${bitmap.height}")
            val resultBitmap = Bitmap.createScaledBitmap(
                outputBitmap,
                bitmap.width,
                bitmap.height,
                true
            )
            
            if (scaledBitmap != bitmap) scaledBitmap.recycle()
            outputBitmap.recycle()
            
            android.util.Log.d("StyleTransfer", "applyStyle: SUCCESS!")
            resultBitmap
        } catch (e: Exception) {
            android.util.Log.e("StyleTransfer", "applyStyle: ERROR - falling back", e)
            applyFallbackStyle(bitmap, currentStyle)
        }
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
                val r = (floatArray[y][x][0] * 255).toInt().coerceIn(0, 255)
                val g = (floatArray[y][x][1] * 255).toInt().coerceIn(0, 255)
                val b = (floatArray[y][x][2] * 255).toInt().coerceIn(0, 255)
                
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
