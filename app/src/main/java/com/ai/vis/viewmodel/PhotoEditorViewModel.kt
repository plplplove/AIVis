package com.ai.vis.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.vis.domain.model.AIStyle
import com.ai.vis.domain.model.EditorTool
import com.ai.vis.domain.model.EditorUiState
import com.ai.vis.domain.model.StickerItem
import com.ai.vis.domain.model.TextItem
import com.ai.vis.domain.usecase.ApplyAIStyleUseCase
import com.ai.vis.domain.usecase.ApplyAdjustmentsUseCase
import com.ai.vis.domain.usecase.ApplyCropRotateUseCase
import com.ai.vis.domain.usecase.ApplyFilterUseCase
import com.ai.vis.domain.usecase.InsertPhotoUseCase
import com.ai.vis.domain.usecase.ProcessBackgroundUseCase
import com.ai.vis.domain.usecase.ProcessPortraitUseCase
import com.ai.vis.data.EditedPhoto
import com.ai.vis.ui.components.BackgroundOption
import com.ai.vis.ui.components.DrawPath
import com.ai.vis.ui.components.FilterType
import com.ai.vis.ui.components.PortraitOption
import com.ai.vis.utils.PhotoSaver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PhotoEditorViewModel(
    private val applyAdjustmentsUseCase: ApplyAdjustmentsUseCase,
    private val applyFilterUseCase: ApplyFilterUseCase,
    private val processPortraitUseCase: ProcessPortraitUseCase,
    private val applyAIStyleUseCase: ApplyAIStyleUseCase,
    private val processBackgroundUseCase: ProcessBackgroundUseCase,
    private val applyCropRotateUseCase: ApplyCropRotateUseCase,
    private val insertPhotoUseCase: InsertPhotoUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    
    private val undoStack = mutableListOf<EditorUiState>()
    private val redoStack = mutableListOf<EditorUiState>()
    
    private val maxUndoStackSize = 20
    
    init {
        viewModelScope.launch {
            processPortraitUseCase.initialize()
            processBackgroundUseCase.initialize()
        }
    }
    
    fun loadImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Loading image...") }
            
            try {
                val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                
                if (bitmap != null) {
                    _uiState.update { 
                        it.copy(
                            originalBitmap = bitmap,
                            currentBitmap = bitmap,
                            displayBitmap = bitmap,
                            isLoading = false,
                            error = null
                        )
                    }
                    saveToUndoStack()
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Failed to load image"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Error loading image: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun selectTool(tool: EditorTool) {
        _uiState.update { it.copy(selectedTool = tool) }
    }
    
    fun updateAdjustment(index: Int, value: Float) {
        val currentValues = _uiState.value.adjustmentValues.toMutableMap()
        currentValues[index] = value
        
        viewModelScope.launch {
            val original = _uiState.value.originalBitmap ?: return@launch
            
            try {
                val result = applyAdjustmentsUseCase(
                    bitmap = original,
                    brightness = currentValues[0] ?: 0f,
                    contrast = currentValues[1] ?: 0f,
                    saturation = currentValues[2] ?: 0f,
                    sharpness = currentValues[3] ?: 0f,
                    temperature = currentValues[4] ?: 0f,
                    tint = currentValues[5] ?: 0f
                )
                
                _uiState.update { 
                    it.copy(
                        displayBitmap = result,
                        adjustmentValues = currentValues
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Adjustment failed: ${e.message}") }
            }
        }
    }
    
    fun applyAdjustments() {
        val display = _uiState.value.displayBitmap ?: return
        
        _uiState.update { 
            it.copy(
                originalBitmap = display,
                currentBitmap = display,
                adjustmentValues = mapOf(
                    0 to 0f, 1 to 0f, 2 to 0f,
                    3 to 0f, 4 to 0f, 5 to 0f
                )
            )
        }
        saveToUndoStack()
    }
    
    fun resetAdjustments() {
        val original = _uiState.value.originalBitmap ?: return
        
        _uiState.update { 
            it.copy(
                displayBitmap = original,
                adjustmentValues = mapOf(
                    0 to 0f, 1 to 0f, 2 to 0f,
                    3 to 0f, 4 to 0f, 5 to 0f
                )
            )
        }
    }
    
    fun selectFilter(filter: FilterType, intensity: Float = 1f) {
        viewModelScope.launch {
            val original = _uiState.value.originalBitmap ?: return@launch
            
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Applying filter...") }
            
            try {
                val result = applyFilterUseCase(original, filter, intensity)
                
                _uiState.update { 
                    it.copy(
                        displayBitmap = result,
                        currentFilter = filter,
                        filterIntensity = intensity,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Filter failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun applyFilter() {
        val display = _uiState.value.displayBitmap ?: return
        
        _uiState.update { 
            it.copy(
                originalBitmap = display,
                currentBitmap = display
            )
        }
        saveToUndoStack()
    }
    
    fun resetFilter() {
        val original = _uiState.value.originalBitmap ?: return
        
        _uiState.update { 
            it.copy(
                displayBitmap = original,
                currentFilter = FilterType.NONE,
                filterIntensity = 1f
            )
        }
    }
    
    fun selectPortraitOption(option: PortraitOption, intensity: Float) {
        viewModelScope.launch {
            val original = _uiState.value.originalBitmap ?: return@launch
            
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Processing portrait...") }
            
            try {
                val result = processPortraitUseCase(
                    bitmap = original,
                    option = option,
                    beautyIntensity = if (option == PortraitOption.BEAUTY_MODE) intensity else _uiState.value.beautyIntensity,
                    eyeIntensity = if (option == PortraitOption.EYE_ENHANCEMENT) intensity else _uiState.value.eyeIntensity,
                    blurIntensity = if (option == PortraitOption.FACE_BLUR) intensity else _uiState.value.blurFaceIntensity
                )
                
                _uiState.update { 
                    it.copy(
                        displayBitmap = result,
                        selectedPortraitOption = option,
                        beautyIntensity = if (option == PortraitOption.BEAUTY_MODE) intensity else it.beautyIntensity,
                        eyeIntensity = if (option == PortraitOption.EYE_ENHANCEMENT) intensity else it.eyeIntensity,
                        blurFaceIntensity = if (option == PortraitOption.FACE_BLUR) intensity else it.blurFaceIntensity,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Portrait processing failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun applyPortrait() {
        val display = _uiState.value.displayBitmap ?: return
        
        _uiState.update { 
            it.copy(
                originalBitmap = display,
                currentBitmap = display,
                selectedPortraitOption = PortraitOption.NONE
            )
        }
        saveToUndoStack()
    }
    
    fun resetPortrait() {
        val original = _uiState.value.originalBitmap ?: return
        
        _uiState.update { 
            it.copy(
                displayBitmap = original,
                selectedPortraitOption = PortraitOption.NONE
            )
        }
    }
    
    fun selectAIStyle(style: AIStyle) {
        viewModelScope.launch {
            val original = _uiState.value.originalBitmap ?: return@launch
            
            if (style == AIStyle.NONE) {
                _uiState.update { 
                    it.copy(
                        displayBitmap = original,
                        selectedAIStyle = AIStyle.NONE
                    )
                }
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Applying AI style...") }
            
            try {
                val result = applyAIStyleUseCase(original, style)
                
                _uiState.update { 
                    it.copy(
                        displayBitmap = result,
                        selectedAIStyle = style,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "AI style failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun applyAIStyle() {
        val display = _uiState.value.displayBitmap ?: return
        
        _uiState.update { 
            it.copy(
                originalBitmap = display,
                currentBitmap = display,
                selectedAIStyle = AIStyle.NONE
            )
        }
        saveToUndoStack()
    }
    
    fun selectBackgroundOption(option: BackgroundOption, blurRadius: Int = 25) {
        viewModelScope.launch {
            val original = _uiState.value.originalBitmap ?: return@launch
            
            if (option == BackgroundOption.NONE) {
                _uiState.update { 
                    it.copy(
                        displayBitmap = original,
                        selectedBackgroundOption = BackgroundOption.NONE
                    )
                }
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Processing background...") }
            
            try {
                val result = processBackgroundUseCase(
                    bitmap = original,
                    option = option,
                    blurRadius = blurRadius,
                    backgroundColor = _uiState.value.backgroundColor,
                    backgroundImage = _uiState.value.backgroundImage
                )
                
                _uiState.update { 
                    it.copy(
                        displayBitmap = result,
                        selectedBackgroundOption = option,
                        backgroundBlurRadius = blurRadius,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Background processing failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun applyBackground() {
        val display = _uiState.value.displayBitmap ?: return
        
        _uiState.update { 
            it.copy(
                originalBitmap = display,
                currentBitmap = display,
                selectedBackgroundOption = BackgroundOption.NONE
            )
        }
        saveToUndoStack()
    }
    
    fun setBackgroundColor(color: Int) {
        _uiState.update { it.copy(backgroundColor = color) }
    }
    
    fun setBackgroundImage(bitmap: Bitmap) {
        _uiState.update { it.copy(backgroundImage = bitmap) }
    }
    
    fun addText(text: String, position: Offset, fontSize: Int, color: Color) {
        val newItem = TextItem(
            id = System.currentTimeMillis(),
            text = text,
            position = position,
            fontSize = fontSize,
            color = color
        )
        
        _uiState.update { 
            it.copy(
                textItems = it.textItems + newItem,
                selectedTextId = newItem.id
            )
        }
        saveToUndoStack()
    }
    
    fun updateTextPosition(id: Long, position: Offset) {
        _uiState.update { state ->
            state.copy(
                textItems = state.textItems.map { item ->
                    if (item.id == id) item.copy(position = position) else item
                }
            )
        }
    }
    
    fun updateTextScale(id: Long, scale: Float) {
        _uiState.update { state ->
            state.copy(
                textItems = state.textItems.map { item ->
                    if (item.id == id) item.copy(scale = scale) else item
                }
            )
        }
    }
    
    fun updateTextRotation(id: Long, rotation: Float) {
        _uiState.update { state ->
            state.copy(
                textItems = state.textItems.map { item ->
                    if (item.id == id) item.copy(rotation = rotation) else item
                }
            )
        }
    }
    
    fun selectText(id: Long?) {
        _uiState.update { it.copy(selectedTextId = id) }
    }
    
    fun deleteText(id: Long) {
        _uiState.update { 
            it.copy(
                textItems = it.textItems.filter { item -> item.id != id },
                selectedTextId = if (it.selectedTextId == id) null else it.selectedTextId
            )
        }
        saveToUndoStack()
    }
    
    fun addSticker(emoji: String, position: Offset) {
        val newItem = StickerItem(
            id = System.currentTimeMillis(),
            emoji = emoji,
            position = position
        )
        
        _uiState.update { 
            it.copy(
                stickerItems = it.stickerItems + newItem,
                selectedStickerId = newItem.id
            )
        }
        saveToUndoStack()
    }
    
    fun updateStickerPosition(id: Long, position: Offset) {
        _uiState.update { state ->
            state.copy(
                stickerItems = state.stickerItems.map { item ->
                    if (item.id == id) item.copy(position = position) else item
                }
            )
        }
    }
    
    fun updateStickerScale(id: Long, scale: Float) {
        _uiState.update { state ->
            state.copy(
                stickerItems = state.stickerItems.map { item ->
                    if (item.id == id) item.copy(scale = scale) else item
                }
            )
        }
    }
    
    fun updateStickerRotation(id: Long, rotation: Float) {
        _uiState.update { state ->
            state.copy(
                stickerItems = state.stickerItems.map { item ->
                    if (item.id == id) item.copy(rotation = rotation) else item
                }
            )
        }
    }
    
    fun selectSticker(id: Long?) {
        _uiState.update { it.copy(selectedStickerId = id) }
    }
    
    fun deleteSticker(id: Long) {
        _uiState.update { 
            it.copy(
                stickerItems = it.stickerItems.filter { item -> item.id != id },
                selectedStickerId = if (it.selectedStickerId == id) null else it.selectedStickerId
            )
        }
        saveToUndoStack()
    }
    
    fun addDrawPath(path: DrawPath) {
        _uiState.update { 
            it.copy(drawPaths = it.drawPaths + path)
        }
    }
    
    fun finishDrawing() {
        saveToUndoStack()
    }
    
    fun setDrawColor(color: Color) {
        _uiState.update { it.copy(currentDrawColor = color) }
    }
    
    fun setDrawWidth(width: Float) {
        _uiState.update { it.copy(currentDrawWidth = width) }
    }
    
    fun setErasing(erasing: Boolean) {
        _uiState.update { it.copy(isErasing = erasing) }
    }
    
    fun clearDrawing() {
        _uiState.update { it.copy(drawPaths = emptyList()) }
        saveToUndoStack()
    }
    
    fun setCropRatio(ratio: String) {
        _uiState.update { it.copy(cropRatio = ratio) }
    }
    
    fun applyCrop(cropRect: Rect) {
        viewModelScope.launch {
            val current = _uiState.value.currentBitmap ?: return@launch
            
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Cropping...") }
            
            try {
                val result = applyCropRotateUseCase.crop(current, cropRect)
                
                _uiState.update { 
                    it.copy(
                        originalBitmap = result,
                        currentBitmap = result,
                        displayBitmap = result,
                        isLoading = false
                    )
                }
                saveToUndoStack()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Crop failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun rotateImage(degrees: Float) {
        viewModelScope.launch {
            val current = _uiState.value.currentBitmap ?: return@launch
            
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Rotating...") }
            
            try {
                val result = applyCropRotateUseCase.rotate(current, degrees)
                
                _uiState.update { 
                    it.copy(
                        originalBitmap = result,
                        currentBitmap = result,
                        displayBitmap = result,
                        rotation = (it.rotation + degrees) % 360,
                        isLoading = false
                    )
                }
                saveToUndoStack()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Rotation failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun flipImage(horizontal: Boolean = false, vertical: Boolean = false) {
        viewModelScope.launch {
            val current = _uiState.value.currentBitmap ?: return@launch
            
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Flipping...") }
            
            try {
                val result = applyCropRotateUseCase.flip(current, horizontal, vertical)
                
                _uiState.update { 
                    it.copy(
                        originalBitmap = result,
                        currentBitmap = result,
                        displayBitmap = result,
                        flipHorizontal = if (horizontal) !it.flipHorizontal else it.flipHorizontal,
                        flipVertical = if (vertical) !it.flipVertical else it.flipVertical,
                        isLoading = false
                    )
                }
                saveToUndoStack()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Flip failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun saveToUndoStack() {
        val currentState = _uiState.value
        
        undoStack.add(currentState)
        
        if (undoStack.size > maxUndoStackSize) {
            undoStack.removeAt(0)
        }
        
        redoStack.clear()
        
        _uiState.update { 
            it.copy(
                canUndo = true,
                canRedo = false
            )
        }
    }
    
    fun undo() {
        if (undoStack.isEmpty()) return
        
        val currentState = _uiState.value
        redoStack.add(currentState)
        
        val previousState = undoStack.removeLast()
        _uiState.value = previousState.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = true
        )
    }
    
    fun redo() {
        if (redoStack.isEmpty()) return
        
        val currentState = _uiState.value
        undoStack.add(currentState)
        
        val nextState = redoStack.removeLast()
        _uiState.value = nextState.copy(
            canUndo = true,
            canRedo = redoStack.isNotEmpty()
        )
    }
    
    fun showSaveDialog(show: Boolean) {
        _uiState.update { it.copy(showSaveDialog = show) }
    }
    
    fun showExitDialog(show: Boolean) {
        _uiState.update { it.copy(showExitDialog = show) }
    }
    
    fun saveImage(context: Context, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val bitmap = _uiState.value.currentBitmap ?: return@launch
            
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    loadingMessage = "Saving...",
                    showSaveDialog = false
                )
            }
            
            try {
                val fileName = "AIVis_${System.currentTimeMillis()}.jpg"
                val uri = PhotoSaver.saveToGallery(context, bitmap, fileName)
                val file = PhotoSaver.saveToAppStorage(context, bitmap, fileName)
                
                if (uri != null && file != null) {
                    val editedPhoto = EditedPhoto(
                        fileName = fileName,
                        filePath = file.absolutePath,
                        timestamp = System.currentTimeMillis(),
                        width = bitmap.width,
                        height = bitmap.height,
                        sizeBytes = file.length()
                    )
                    
                    insertPhotoUseCase(editedPhoto)
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            saveSuccess = true,
                            saveMessage = "Saved: $fileName"
                        )
                    }
                    
                    onSuccess(fileName)
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Failed to save image"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Save failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false, saveMessage = "") }
    }
    
    override fun onCleared() {
        super.onCleared()
        processPortraitUseCase.release()
        processBackgroundUseCase.release()
    }
}
