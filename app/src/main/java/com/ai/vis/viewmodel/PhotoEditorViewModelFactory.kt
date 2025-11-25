package com.ai.vis.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ai.vis.data.AppDatabase
import com.ai.vis.data.PhotoRepository
import com.ai.vis.domain.usecase.ApplyAIStyleUseCase
import com.ai.vis.domain.usecase.ApplyAdjustmentsUseCase
import com.ai.vis.domain.usecase.ApplyCropRotateUseCase
import com.ai.vis.domain.usecase.ApplyFilterUseCase
import com.ai.vis.domain.usecase.InsertPhotoUseCase
import com.ai.vis.domain.usecase.ProcessBackgroundUseCase
import com.ai.vis.domain.usecase.ProcessPortraitUseCase

class PhotoEditorViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoEditorViewModel::class.java)) {
            val photoDao = AppDatabase.getDatabase(context).editedPhotoDao()
            val repository = PhotoRepository(photoDao)
            
            val applyAdjustmentsUseCase = ApplyAdjustmentsUseCase()
            val applyFilterUseCase = ApplyFilterUseCase()
            val processPortraitUseCase = ProcessPortraitUseCase(context)
            val applyAIStyleUseCase = ApplyAIStyleUseCase(context)
            val processBackgroundUseCase = ProcessBackgroundUseCase(context)
            val applyCropRotateUseCase = ApplyCropRotateUseCase()
            val insertPhotoUseCase = InsertPhotoUseCase(repository)
            
            return PhotoEditorViewModel(
                applyAdjustmentsUseCase,
                applyFilterUseCase,
                processPortraitUseCase,
                applyAIStyleUseCase,
                processBackgroundUseCase,
                applyCropRotateUseCase,
                insertPhotoUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
