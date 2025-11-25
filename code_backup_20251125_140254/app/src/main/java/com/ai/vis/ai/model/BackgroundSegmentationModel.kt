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

/**
 * Background segmentation model using TensorFlow Lite.
 * Uses a segmentation model (e.g., DeepLab, MediaPipe) to create a mask
 * that separates foreground (person) from background.
 */
class BackgroundSegmentationModel(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private var isInitialized = false
    
    // Model input/output dimensions - will be adjusted based on actual model
    private var inputSize = 256
    
    companion object {
        private const val TAG = "BackgroundSegmentation"
        private const val MODEL_PATH = "models/selfie_segmentation.tflite"
    }
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isInitialized) {
            return@withContext
        }
        
        try {
            Log.d(TAG, "Loading segmentation model from: $MODEL_PATH")
            val model = loadModelFromAssets(MODEL_PATH)
            val options = Interpreter.Options().apply {
                setNumThreads(Runtime.getRuntime().availableProcessors())
                setUseXNNPACK(true)
            }
            interpreter = Interpreter(model, options)
            isInitialized = true
            
            interpreter?.let { interp ->
                Log.d(TAG, "✅ Model loaded successfully!")
                Log.d(TAG, "Input count: ${interp.inputTensorCount}")
                Log.d(TAG, "Output count: ${interp.outputTensorCount}")
                
                // Get actual input size from model
                val inputShape = interp.getInputTensor(0).shape()
                val modelInputSize = inputShape[1] // [1, height, width, 3]
                inputSize = modelInputSize
                
                Log.d(TAG, "Input shape: ${inputShape.contentToString()}")
                Log.d(TAG, "Output shape: ${interp.getOutputTensor(0).shape().contentToString()}")
                Log.d(TAG, "Adjusted inputSize to: $inputSize")
            }
        } catch (e: java.io.FileNotFoundException) {
            Log.e(TAG, "❌ MODEL FILE NOT FOUND: $MODEL_PATH")
            Log.e(TAG, "Please place 'selfie_segmentation.tflite' in app/src/main/assets/models/")
            Log.e(TAG, "See BACKGROUND_MODEL_SETUP.md for instructions")
            Log.w(TAG, "⚠️ Will use algorithmic fallback instead")
            isInitialized = false
            // Don't throw - allow fallback to work
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing model: ${e.message}", e)
            Log.w(TAG, "⚠️ Will use algorithmic fallback instead")
            isInitialized = false
            // Don't throw - allow fallback to work
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
    
    /**
     * Segments the image and returns a mask where:
     * - 1.0 = foreground (person)
     * - 0.0 = background
     * 
     * The mask is the same size as the input bitmap.
     */
    suspend fun segmentImage(bitmap: Bitmap): Array<FloatArray>? = withContext(Dispatchers.IO) {
        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "Model not initialized, using algorithmic fallback")
            return@withContext createAlgorithmicMask(bitmap)
        }
        
        try {
            Log.d(TAG, "Segmenting image ${bitmap.width}x${bitmap.height}")
            
            // Resize bitmap to model input size
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            
            // Convert to input buffer
            val inputBuffer = bitmapToByteBuffer(resizedBitmap)
            
            // Check output shape to determine model type
            val outputTensor = interpreter?.getOutputTensor(0)
            val outputShape = outputTensor?.shape()
            val numClasses = outputShape?.get(3) ?: 1
            
            Log.d(TAG, "Output has $numClasses classes")
            
            // Prepare output based on number of classes
            val outputArray = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(numClasses) } } }
            
            // Run inference
            interpreter?.run(inputBuffer, outputArray)
            
            // Extract mask and resize to original bitmap size
            val mask = Array(bitmap.height) { FloatArray(bitmap.width) }
            
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    // Map coordinates from original size to model output size
                    val srcY = (y.toFloat() / bitmap.height * inputSize).toInt().coerceIn(0, inputSize - 1)
                    val srcX = (x.toFloat() / bitmap.width * inputSize).toInt().coerceIn(0, inputSize - 1)
                    
                    // Get mask value based on model type
                    val maskValue = if (numClasses == 1) {
                        // Simple segmentation model (MediaPipe)
                        outputArray[0][srcY][srcX][0]
                    } else {
                        // Multi-class segmentation (DeepLab)
                        // Find the class with highest probability
                        val classProbs = outputArray[0][srcY][srcX]
                        var maxProb = 0f
                        var maxClass = 0
                        
                        for (c in 0 until numClasses) {
                            if (classProbs[c] > maxProb) {
                                maxProb = classProbs[c]
                                maxClass = c
                            }
                        }
                        
                        // Class 15 is "person" in PASCAL VOC (DeepLab)
                        // If max class is person, use high confidence, otherwise low
                        if (maxClass == 15) maxProb else 0f
                    }
                    
                    mask[y][x] = maskValue.coerceIn(0f, 1f)
                }
            }
            
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            
            // Log mask statistics for debugging
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
            
            Log.d(TAG, "Segmentation complete - Mask stats: min=$minVal, max=$maxVal, avg=$avgVal")
            mask
        } catch (e: Exception) {
            Log.e(TAG, "Error during segmentation: ${e.message}", e)
            Log.w(TAG, "Falling back to algorithmic mask")
            createAlgorithmicMask(bitmap)
        }
    }
    
    /**
     * Refines the mask using edge-aware smoothing to reduce jagged edges.
     */
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
    
    /**
     * Algorithmic fallback: Creates a simple mask based on center focus
     * Assumes subject is in the center of the image
     */
    private fun createAlgorithmicMask(bitmap: Bitmap): Array<FloatArray> {
        Log.d(TAG, "Creating algorithmic mask (center-weighted)")
        val width = bitmap.width
        val height = bitmap.height
        val mask = Array(height) { FloatArray(width) }
        
        val centerX = width / 2f
        val centerY = height / 2f
        val maxDist = kotlin.math.sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Distance from center
                val dx = x - centerX
                val dy = y - centerY
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                
                // Normalized distance (0 at center, 1 at corners)
                val normDist = (dist / maxDist).coerceIn(0f, 1f)
                
                // Inverse: 1 at center (foreground), 0 at edges (background)
                // Using smooth falloff
                mask[y][x] = (1f - normDist * normDist).coerceIn(0f, 1f)
            }
        }
        
        return mask
    }
    
    fun release() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
        Log.d(TAG, "Model released")
    }
}
