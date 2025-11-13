package com.ai.vis.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai.vis.data.AppDatabase
import com.ai.vis.data.EditedPhoto
import com.ai.vis.data.PhotoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotoGalleryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: PhotoRepository
    val allPhotos: StateFlow<List<EditedPhoto>>
    
    init {
        val photoDao = AppDatabase.getDatabase(application).editedPhotoDao()
        repository = PhotoRepository(photoDao)
        allPhotos = repository.allPhotos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
    
    fun insertPhoto(photo: EditedPhoto, onSuccess: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertPhoto(photo)
            onSuccess(id)
        }
    }
    
    fun deletePhoto(photo: EditedPhoto) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
        }
    }
    
    fun deletePhotoById(photoId: Long) {
        viewModelScope.launch {
            repository.deletePhotoById(photoId)
        }
    }
    
    fun deleteAllPhotos() {
        viewModelScope.launch {
            repository.deleteAllPhotos()
        }
    }
    
    fun updatePhotoFileName(photoId: Long, newFileName: String) {
        viewModelScope.launch {
            repository.updatePhotoFileName(photoId, newFileName)
        }
    }
}
