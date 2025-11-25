package com.ai.vis.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai.vis.data.AppDatabase
import com.ai.vis.data.EditedPhoto
import com.ai.vis.data.PhotoRepository
import com.ai.vis.domain.usecase.DeletePhotoUseCase
import com.ai.vis.domain.usecase.GetAllPhotosUseCase
import com.ai.vis.domain.usecase.InsertPhotoUseCase
import com.ai.vis.domain.usecase.UpdatePhotoFileNameUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotoGalleryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val getAllPhotosUseCase: GetAllPhotosUseCase
    private val insertPhotoUseCase: InsertPhotoUseCase
    private val deletePhotoUseCase: DeletePhotoUseCase
    private val updatePhotoFileNameUseCase: UpdatePhotoFileNameUseCase
    
    val allPhotos: StateFlow<List<EditedPhoto>>
    
    init {
        val photoDao = AppDatabase.getDatabase(application).editedPhotoDao()
        val repository = PhotoRepository(photoDao)
        
        getAllPhotosUseCase = GetAllPhotosUseCase(repository)
        insertPhotoUseCase = InsertPhotoUseCase(repository)
        deletePhotoUseCase = DeletePhotoUseCase(repository)
        updatePhotoFileNameUseCase = UpdatePhotoFileNameUseCase(repository)
        
        allPhotos = getAllPhotosUseCase().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
    
    fun insertPhoto(photo: EditedPhoto, onSuccess: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = insertPhotoUseCase(photo)
            onSuccess(id)
        }
    }
    
    fun deletePhoto(photo: EditedPhoto) {
        viewModelScope.launch {
            deletePhotoUseCase(photo)
        }
    }
    
    fun deletePhotoById(photoId: Long) {
        viewModelScope.launch {
            deletePhotoUseCase.byId(photoId)
        }
    }
    
    fun deleteAllPhotos() {
        viewModelScope.launch {
            val photos = allPhotos.value
            photos.forEach { photo ->
                deletePhotoUseCase(photo)
            }
        }
    }
    
    fun updatePhotoFileName(photoId: Long, newFileName: String) {
        viewModelScope.launch {
            updatePhotoFileNameUseCase(photoId, newFileName)
        }
    }
}
