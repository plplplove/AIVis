package com.ai.vis.data

import kotlinx.coroutines.flow.Flow

class PhotoRepository(private val photoDao: EditedPhotoDao) {
    
    val allPhotos: Flow<List<EditedPhoto>> = photoDao.getAllPhotos()
    
    suspend fun insertPhoto(photo: EditedPhoto): Long {
        return photoDao.insertPhoto(photo)
    }
    
    suspend fun deletePhoto(photo: EditedPhoto) {
        photoDao.deletePhoto(photo)
    }
    
    suspend fun deletePhotoById(photoId: Long) {
        photoDao.deletePhotoById(photoId)
    }
    
    suspend fun deleteAllPhotos() {
        photoDao.deleteAllPhotos()
    }
    
    suspend fun getPhotoById(photoId: Long): EditedPhoto? {
        return photoDao.getPhotoById(photoId)
    }
    
    suspend fun getPhotosCount(): Int {
        return photoDao.getPhotosCount()
    }
    
    suspend fun updatePhotoFileName(photoId: Long, newFileName: String) {
        photoDao.updatePhotoFileName(photoId, newFileName)
    }
}
