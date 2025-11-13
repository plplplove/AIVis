package com.ai.vis.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.ai.vis.data.EditedPhoto
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PhotoSaver {
    
    private const val TAG = "PhotoSaver"
    private const val AIVIS_FOLDER = "AIVis"
    
    /**
     * Зберегти фото в галерею пристрою через MediaStore
     */
    fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "AIVis_${System.currentTimeMillis()}.jpg",
        quality: Int = 95
    ): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$AIVIS_FOLDER")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            
            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(it, contentValues, null, null)
                }
                
                Log.d(TAG, "Photo saved to gallery: $it")
                it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving photo to gallery", e)
            null
        }
    }
    
    /**
     * Зберегти фото локально в папку додатку
     */
    fun saveToAppStorage(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "AIVis_${System.currentTimeMillis()}.jpg",
        quality: Int = 95
    ): File? {
        return try {
            val directory = File(context.filesDir, "edited_photos")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val file = File(directory, fileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }
            
            Log.d(TAG, "Photo saved to app storage: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error saving photo to app storage", e)
            null
        }
    }
    
    /**
     * Створити thumbnail для фото
     */
    fun createThumbnail(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "thumb_${System.currentTimeMillis()}.jpg",
        maxSize: Int = 300
    ): File? {
        return try {
            val directory = File(context.filesDir, "thumbnails")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val scale = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()
            
            val thumbnail = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            
            val file = File(directory, fileName)
            FileOutputStream(file).use { outputStream ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            }
            
            if (thumbnail != bitmap) {
                thumbnail.recycle()
            }
            
            Log.d(TAG, "Thumbnail created: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error creating thumbnail", e)
            null
        }
    }
    
    /**
     * Видалити фото з файлової системи
     */
    fun deletePhotoFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting photo file", e)
            false
        }
    }
    
    /**
     * Видалити всі фото з папки додатку
     */
    fun deleteAllAppPhotos(context: Context): Int {
        var deletedCount = 0
        try {
            val directory = File(context.filesDir, "edited_photos")
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles()?.forEach { file ->
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            // Also delete thumbnails
            val thumbDirectory = File(context.filesDir, "thumbnails")
            if (thumbDirectory.exists() && thumbDirectory.isDirectory) {
                thumbDirectory.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all app photos", e)
        }
        return deletedCount
    }
    
    /**
     * Отримати розмір файлу в байтах
     */
    fun getFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            0L
        }
    }
}
