package com.ai.vis.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EditedPhotoDao {
    @Query("SELECT * FROM edited_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<EditedPhoto>>
    
    @Query("SELECT * FROM edited_photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: Long): EditedPhoto?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: EditedPhoto): Long
    
    @Delete
    suspend fun deletePhoto(photo: EditedPhoto)
    
    @Query("DELETE FROM edited_photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: Long)
    
    @Query("DELETE FROM edited_photos")
    suspend fun deleteAllPhotos()
    
    @Query("SELECT COUNT(*) FROM edited_photos")
    suspend fun getPhotosCount(): Int
    
    @Query("UPDATE edited_photos SET fileName = :newFileName WHERE id = :photoId")
    suspend fun updatePhotoFileName(photoId: Long, newFileName: String)
}
