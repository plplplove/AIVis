package com.ai.vis.ai.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class BackgroundSegmentationModel(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private var isInitialized = false
    
    private var inputSize = 256
    
    companion object {
        private const val MODEL_PATH = "models/selfie_segmentation.tflite"
    }
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isInitialized) {
            return@withContext
        }
        
        try {
            val model = loadModelFromAssets(MODEL_PATH)
            val options = Interpreter.Options().apply {
                setNumThreads(Runtime.getRuntime().availableProcessors())
                setUseXNNPACK(true)
            }
            interpreter = Interpreter(model, options)
            isInitialized = true
            
            interpreter?.let { interp ->
                
                val inputShape = interp.getInputTensor(0).shape()
                val modelInputSize = inputShape[1] 
                inputSize = modelInputSize
                
            }
        } catch (e: java.io.FileNotFoundException) {
            isInitialized = false
        } catch (e: Exception) {
            isInitialized = false
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
    
    suspend fun segmentImage(bitmap: Bitmap): Array<FloatArray>? = withContext(Dispatchers.IO) {
        if (!isInitialized || interpreter == null) {
            return@withContext createAlgorithmicMask(bitmap)
        }
        
        try {
            
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            
            val inputBuffer = bitmapToByteBuffer(resizedBitmap)
            
            val outputTensor = interpreter?.getOutputTensor(0)
            val outputShape = outputTensor?.shape()
            val numClasses = outputShape?.get(3) ?: 1
            
            
            val outputArray = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(numClasses) } } }
            
            interpreter?.run(inputBuffer, outputArray)
            
            val mask = Array(bitmap.height) { FloatArray(bitmap.width) }
            
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    val srcY = (y.toFloat() / bitmap.height * inputSize).toInt().coerceIn(0, inputSize - 1)
                    val srcX = (x.toFloat() / bitmap.width * inputSize).toInt().coerceIn(0, inputSize - 1)
                    
                    val maskValue = if (numClasses == 1) {
                        outputArray[0][srcY][srcX][0]
                    } else {
                        val classProbs = outputArray[0][srcY][srcX]
                        var maxProb = 0f
                        var maxClass = 0
                        
                        for (c in 0 until numClasses) {
                            if (classProbs[c] > maxProb) {
                                maxProb = classProbs[c]
                                maxClass = c
                            }
                        }
                        
                        if (maxClass == 15) maxProb else 0f
                    }
                    
                    mask[y][x] = maskValue.coerceIn(0f, 1f)
                }
            }
            
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            
            var minVal = 1f
            var maxVal = 0f
            var avgVal = 0f
            var count = 0
            for (row in mask) {
                for (value in row) {
                    minVal = minOf(minVal, value)
                    maxVal = maxOf(maxVal, value)
                    avgVal += value
                    count++
                }
            }
            avgVal /= count
            
            mask
        } catch (e: Exception) {
            createAlgorithmicMask(bitmap)
        }
    }
    
    fun refineMask(mask: Array<FloatArray>, radius: Int = 2): Array<FloatArray> {
        val height = mask.size
        val width = mask[0].size
        val refined = Array(height) { FloatArray(width) }
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f
                var count = 0
                
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val nx = (x + dx).coerceIn(0, width - 1)
                        sum += mask[ny][nx]
                        count++
                    }
                }
                
                refined[y][x] = (sum / count).coerceIn(0f, 1f)
            }
        }
        
        return refined
    }
    
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        
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
    
    private fun createAlgorithmicMask(bitmap: Bitmap): Array<FloatArray> {
        val width = bitmap.width
        val height = bitmap.height
        val mask = Array(height) { FloatArray(width) }
        
        val centerX = width / 2f
        val centerY = height / 2f
        val maxDist = kotlin.math.sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - centerX
                val dy = y - centerY
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                
                val normDist = (dist / maxDist).coerceIn(0f, 1f)
                
                mask[y][x] = (1f - normDist * normDist).coerceIn(0f, 1f)
            }
        }
        
        return mask
    }
    
    fun release() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
}
