package com.ai.vis.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.ai.vis.R
import com.ai.vis.ui.components.CropRatio
import com.ai.vis.ui.theme.AIVisTheme
import com.ai.vis.utils.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorTool(
    val nameRes: Int,
    val iconRes: Int
)

data class TextItem(
    val id: Int,
    var position: Offset,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var style: com.ai.vis.ui.components.TextStyle
)

data class StickerItem(
    val id: Int,
    var emoji: String,
    var position: Offset,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var opacity: Float = 1f
)

data class EditorState(
    val bitmap: Bitmap,
    val textItems: List<TextItem> = emptyList(),
    val stickerItems: List<StickerItem> = emptyList(),
    val drawPaths: List<com.ai.vis.ui.components.DrawPath> = emptyList(),
    val adjustmentValues: Map<Int, Float> = mapOf(
        0 to 0f, 1 to 0f, 2 to 0f,
        3 to 0f, 4 to 0f, 5 to 0f, 6 to 0f
    ),
    val currentFilter: com.ai.vis.ui.components.FilterType = com.ai.vis.ui.components.FilterType.NONE,
    val filterIntensity: Float = 1f,
    val selectedAIStyle: com.ai.vis.domain.model.AIStyle = com.ai.vis.domain.model.AIStyle.NONE,
    val selectedBackgroundOption: com.ai.vis.ui.components.BackgroundOption = com.ai.vis.ui.components.BackgroundOption.NONE,
    val selectedPortraitOption: com.ai.vis.ui.components.PortraitOption = com.ai.vis.ui.components.PortraitOption.NONE
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    imageUri: Uri?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    photoGalleryViewModel: com.ai.vis.viewmodel.PhotoGalleryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showExitDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf("") }
    
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) } 
    var displayBitmap = previewBitmap ?: originalBitmap 
    
    var undoStack by remember { mutableStateOf<List<EditorState>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<EditorState>>(emptyList()) }
    
    var savedStatesUndoStack by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var savedStatesRedoStack by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    
    var stateBeforeEditing by remember { mutableStateOf<Bitmap?>(null) }
    
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += offsetChange
    }
    
    var selectedTool by remember { mutableStateOf<EditorTool?>(null) }
    
    var isEditing by remember { mutableStateOf(false) }
    
    var isToolPanelCollapsed by remember { mutableStateOf(false) }
    
    var selectedCropRatio by remember { mutableStateOf<CropRatio?>(null) }
    var showCropOverlay by remember { mutableStateOf(false) }
    var cropRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var straightenAngle by remember { mutableFloatStateOf(0f) } 
    
    var cropFieldModified by remember { mutableStateOf(false) }
    var straightenModified by remember { mutableStateOf(false) }
    
    var imageBounds by remember(originalBitmap) { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    val density = LocalDensity.current
    
    var bottomPanelsHeight by remember { mutableFloatStateOf(0f) }
    
    var adjustmentValues by remember { mutableStateOf(mapOf(
        0 to 0f, 
        1 to 0f, 
        2 to 0f, 
        3 to 0f, 
        4 to 0f, 
        5 to 0f  
    )) }
    
    var textItems by remember { mutableStateOf<List<TextItem>>(emptyList()) }
    var selectedTextId by remember { mutableStateOf<Int?>(null) }
    var nextTextId by remember { mutableStateOf(0) }
    var showTextDialog by remember { mutableStateOf(false) }
    var dialogInputText by remember { mutableStateOf("") }
    var textStyle by remember { mutableStateOf(com.ai.vis.ui.components.TextStyle()) }
    
    var stickerItems by remember { mutableStateOf<List<StickerItem>>(emptyList()) }
    var selectedStickerId by remember { mutableStateOf<Int?>(null) }
    var nextStickerId by remember { mutableStateOf(0) }
    var stickerSize by remember { mutableFloatStateOf(1.5f) } 
    var stickerOpacity by remember { mutableFloatStateOf(1f) } 
    
    var savedStateForTransform by remember { mutableStateOf(false) }
    
    var drawPaths by remember { mutableStateOf<List<com.ai.vis.ui.components.DrawPath>>(emptyList()) }
    var drawColor by remember { mutableStateOf(Color.Black) }
    var drawStrokeWidth by remember { mutableFloatStateOf(10f) }
    var drawOpacity by remember { mutableFloatStateOf(1f) }
    var drawSoftness by remember { mutableFloatStateOf(0f) }
    var isEraserMode by remember { mutableStateOf(false) }
    var currentShapeType by remember { mutableStateOf(com.ai.vis.ui.components.ShapeType.FREE_DRAW) }
    var isShapeFilled by remember { mutableStateOf(false) }
    
    var currentFilter by remember { mutableStateOf(com.ai.vis.ui.components.FilterType.NONE) }
    var filterIntensity by remember { mutableFloatStateOf(1f) }
    
    var selectedAIStyle by remember { mutableStateOf(com.ai.vis.domain.model.AIStyle.NONE) }
    var isApplyingAIStyle by remember { mutableStateOf(false) }
    var committedAIStyle by remember { mutableStateOf(com.ai.vis.domain.model.AIStyle.NONE) }
    
    var selectedBackgroundOption by remember { mutableStateOf(com.ai.vis.ui.components.BackgroundOption.NONE) }
    var isProcessingBackground by remember { mutableStateOf(false) }
    var blurRadius by remember { mutableStateOf(25f) }
    var selectedBackgroundImage by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    var selectedPortraitOption by remember { mutableStateOf(com.ai.vis.ui.components.PortraitOption.NONE) }
    var isProcessingPortrait by remember { mutableStateOf(false) }
    var beautyIntensity by remember { mutableFloatStateOf(0.5f) }
    var eyeIntensity by remember { mutableFloatStateOf(0.5f) }
    var blurFaceIntensity by remember { mutableFloatStateOf(0.5f) }
    
    val backgroundImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                selectedBackgroundImage = bitmap
                inputStream?.close()
            } catch (e: Exception) {
            }
        }
    }
    
    var imageRectInBox by remember { mutableStateOf<Rect?>(null) }
    
    fun saveStateToUndo() {
        originalBitmap?.let { bitmap ->
            val currentState = EditorState(
                bitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
                textItems = textItems.map { it.copy() },
                stickerItems = stickerItems.map { it.copy() },
                drawPaths = drawPaths.map { it.copy() },
                adjustmentValues = adjustmentValues.toMap(),
                currentFilter = currentFilter,
                filterIntensity = filterIntensity,
                selectedAIStyle = selectedAIStyle,
                selectedBackgroundOption = selectedBackgroundOption,
                selectedPortraitOption = selectedPortraitOption
            )
            undoStack = undoStack + currentState
            redoStack = emptyList() 
        }
    }
    
    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val currentState = originalBitmap?.let { bitmap ->
                EditorState(
                    bitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
                    textItems = textItems.map { it.copy() },
                    stickerItems = stickerItems.map { it.copy() },
                    drawPaths = drawPaths.map { it.copy() },
                    adjustmentValues = adjustmentValues.toMap(),
                    currentFilter = currentFilter,
                    filterIntensity = filterIntensity,
                    selectedAIStyle = selectedAIStyle,
                    selectedBackgroundOption = selectedBackgroundOption,
                    selectedPortraitOption = selectedPortraitOption
                )
            }
            
            val previousState = undoStack.last()
            undoStack = undoStack.dropLast(1)
            
            currentState?.let { redoStack = redoStack + it }
            
            originalBitmap = previousState.bitmap
            textItems = previousState.textItems
            stickerItems = previousState.stickerItems
            drawPaths = previousState.drawPaths
            adjustmentValues = previousState.adjustmentValues
            currentFilter = previousState.currentFilter
            filterIntensity = previousState.filterIntensity
            selectedAIStyle = previousState.selectedAIStyle
            selectedBackgroundOption = previousState.selectedBackgroundOption
            selectedPortraitOption = previousState.selectedPortraitOption
            committedAIStyle = previousState.selectedAIStyle
            
            if (selectedTool?.nameRes == R.string.adjust) {
                coroutineScope.launch(Dispatchers.IO) {
                    originalBitmap?.let { original ->
                        var result = original
                        
                        adjustmentValues[0]?.let { brightness ->
                            if (brightness != 0f) result = ImageProcessor.adjustBrightness(result, brightness)
                        }
                        adjustmentValues[1]?.let { contrast ->
                            if (contrast != 0f) result = ImageProcessor.adjustContrast(result, contrast)
                        }
                        adjustmentValues[2]?.let { saturation ->
                            if (saturation != 0f) result = ImageProcessor.adjustSaturation(result, saturation)
                        }
                        adjustmentValues[3]?.let { sharpness ->
                            if (sharpness != 0f) result = ImageProcessor.adjustSharpness(result, sharpness)
                        }
                        adjustmentValues[4]?.let { temperature ->
                            if (temperature != 0f) result = ImageProcessor.adjustTemperature(result, temperature)
                        }
                        adjustmentValues[5]?.let { tint ->
                            if (tint != 0f) result = ImageProcessor.adjustTint(result, tint)
                        }
                        
                        previewBitmap = result
                    }
                }
            } else if (selectedTool?.nameRes == R.string.filters) {
                coroutineScope.launch(Dispatchers.IO) {
                    originalBitmap?.let { original ->
                        val result = when (currentFilter) {
                            com.ai.vis.ui.components.FilterType.NONE -> original
                            com.ai.vis.ui.components.FilterType.BW -> ImageProcessor.applyBWFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.SEPIA -> ImageProcessor.applySepiaFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.VINTAGE -> ImageProcessor.applyVintageFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.COOL -> ImageProcessor.applyCoolFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.WARM -> ImageProcessor.applyWarmFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.GRAYSCALE -> ImageProcessor.applyGrayscaleFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.INVERT -> ImageProcessor.applyInvertFilter(original, filterIntensity)
                        }
                        previewBitmap = result
                    }
                }
            } else if (selectedTool?.nameRes == R.string.ai_styles) {
                coroutineScope.launch(Dispatchers.IO) {
                    originalBitmap?.let { original ->
                        if (selectedAIStyle == com.ai.vis.domain.model.AIStyle.NONE) {
                            previewBitmap = null
                        } else {
                            try {
                                val applyAIStyleUseCase = com.ai.vis.domain.usecase.ApplyAIStyleUseCase(context)
                                val result = applyAIStyleUseCase(original, selectedAIStyle)
                                withContext(Dispatchers.Main) {
                                    previewBitmap = result
                                }
                                applyAIStyleUseCase.release()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    previewBitmap = originalBitmap
                                }
                            }
                        }
                    }
                }
            } else if (selectedTool?.nameRes == R.string.ai_background) {
                if (selectedBackgroundOption != com.ai.vis.ui.components.BackgroundOption.NONE) {
                    isProcessingBackground = true
                    coroutineScope.launch(Dispatchers.IO) {
                        originalBitmap?.let { original ->
                            try {
                                val processBackgroundUseCase = com.ai.vis.domain.usecase.ProcessBackgroundUseCase(context)
                                processBackgroundUseCase.initialize()
                                val result = processBackgroundUseCase(
                                    bitmap = original,
                                    option = selectedBackgroundOption,
                                    blurRadius = blurRadius.toInt(),
                                    backgroundImage = selectedBackgroundImage
                                )
                                withContext(Dispatchers.Main) {
                                    previewBitmap = result
                                    isProcessingBackground = false
                                }
                                processBackgroundUseCase.release()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    previewBitmap = originalBitmap
                                    isProcessingBackground = false
                                }
                            }
                        }
                    }
                } else {
                    previewBitmap = null
                }
            } else if (selectedTool?.nameRes == R.string.portrait) {
                if (selectedPortraitOption != com.ai.vis.ui.components.PortraitOption.NONE) {
                    isProcessingPortrait = true
                    coroutineScope.launch(Dispatchers.IO) {
                        originalBitmap?.let { original ->
                            try {
                                val processPortraitUseCase = com.ai.vis.domain.usecase.ProcessPortraitUseCase(context)
                                processPortraitUseCase.initialize()
                                val result = processPortraitUseCase(
                                    bitmap = original,
                                    option = selectedPortraitOption,
                                    beautyIntensity = beautyIntensity,
                                    eyeIntensity = eyeIntensity,
                                    blurIntensity = blurFaceIntensity
                                )
                                withContext(Dispatchers.Main) {
                                    previewBitmap = result
                                    isProcessingPortrait = false
                                }
                                processPortraitUseCase.release()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    previewBitmap = originalBitmap
                                    isProcessingPortrait = false
                                }
                            }
                        }
                    }
                } else {
                    previewBitmap = null
                }
            } else {
                previewBitmap = null
            }
        }
    }
    
    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val currentState = originalBitmap?.let { bitmap ->
                EditorState(
                    bitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
                    textItems = textItems.map { it.copy() },
                    stickerItems = stickerItems.map { it.copy() },
                    drawPaths = drawPaths.map { it.copy() },
                    adjustmentValues = adjustmentValues.toMap(),
                    currentFilter = currentFilter,
                    filterIntensity = filterIntensity,
                    selectedAIStyle = selectedAIStyle,
                    selectedBackgroundOption = selectedBackgroundOption,
                    selectedPortraitOption = selectedPortraitOption
                )
            }
            
            val nextState = redoStack.last()
            redoStack = redoStack.dropLast(1)
            
            currentState?.let { undoStack = undoStack + it }
            
            originalBitmap = nextState.bitmap
            textItems = nextState.textItems
            stickerItems = nextState.stickerItems
            drawPaths = nextState.drawPaths
            adjustmentValues = nextState.adjustmentValues
            currentFilter = nextState.currentFilter
            filterIntensity = nextState.filterIntensity
            selectedAIStyle = nextState.selectedAIStyle
            selectedBackgroundOption = nextState.selectedBackgroundOption
            selectedPortraitOption = nextState.selectedPortraitOption
            committedAIStyle = nextState.selectedAIStyle
            
            if (selectedTool?.nameRes == R.string.adjust) {
                coroutineScope.launch(Dispatchers.IO) {
                    originalBitmap?.let { original ->
                        var result = original
                        
                        adjustmentValues[0]?.let { brightness ->
                            if (brightness != 0f) result = ImageProcessor.adjustBrightness(result, brightness)
                        }
                        adjustmentValues[1]?.let { contrast ->
                            if (contrast != 0f) result = ImageProcessor.adjustContrast(result, contrast)
                        }
                        adjustmentValues[2]?.let { saturation ->
                            if (saturation != 0f) result = ImageProcessor.adjustSaturation(result, saturation)
                        }
                        adjustmentValues[3]?.let { sharpness ->
                            if (sharpness != 0f) result = ImageProcessor.adjustSharpness(result, sharpness)
                        }
                        adjustmentValues[4]?.let { temperature ->
                            if (temperature != 0f) result = ImageProcessor.adjustTemperature(result, temperature)
                        }
                        adjustmentValues[5]?.let { tint ->
                            if (tint != 0f) result = ImageProcessor.adjustTint(result, tint)
                        }
                        
                        previewBitmap = result
                    }
                }
            } else if (selectedTool?.nameRes == R.string.filters) {
                coroutineScope.launch(Dispatchers.IO) {
                    originalBitmap?.let { original ->
                        val result = when (currentFilter) {
                            com.ai.vis.ui.components.FilterType.NONE -> original
                            com.ai.vis.ui.components.FilterType.BW -> ImageProcessor.applyBWFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.SEPIA -> ImageProcessor.applySepiaFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.VINTAGE -> ImageProcessor.applyVintageFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.COOL -> ImageProcessor.applyCoolFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.WARM -> ImageProcessor.applyWarmFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.GRAYSCALE -> ImageProcessor.applyGrayscaleFilter(original, filterIntensity)
                            com.ai.vis.ui.components.FilterType.INVERT -> ImageProcessor.applyInvertFilter(original, filterIntensity)
                        }
                        previewBitmap = result
                    }
                }
            } else if (selectedTool?.nameRes == R.string.ai_styles) {
                coroutineScope.launch(Dispatchers.IO) {
                    originalBitmap?.let { original ->
                        if (selectedAIStyle == com.ai.vis.domain.model.AIStyle.NONE) {
                            previewBitmap = null
                        } else {
                            try {
                                val applyAIStyleUseCase = com.ai.vis.domain.usecase.ApplyAIStyleUseCase(context)
                                val result = applyAIStyleUseCase(original, selectedAIStyle)
                                withContext(Dispatchers.Main) {
                                    previewBitmap = result
                                }
                                applyAIStyleUseCase.release()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    previewBitmap = originalBitmap
                                }
                            }
                        }
                    }
                }
            } else if (selectedTool?.nameRes == R.string.ai_background) {
                if (selectedBackgroundOption != com.ai.vis.ui.components.BackgroundOption.NONE) {
                    isProcessingBackground = true
                    coroutineScope.launch(Dispatchers.IO) {
                        originalBitmap?.let { original ->
                            try {
                                val processBackgroundUseCase = com.ai.vis.domain.usecase.ProcessBackgroundUseCase(context)
                                processBackgroundUseCase.initialize()
                                val result = processBackgroundUseCase(
                                    bitmap = original,
                                    option = selectedBackgroundOption,
                                    blurRadius = blurRadius.toInt(),
                                    backgroundImage = selectedBackgroundImage
                                )
                                withContext(Dispatchers.Main) {
                                    previewBitmap = result
                                    isProcessingBackground = false
                                }
                                processBackgroundUseCase.release()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    previewBitmap = originalBitmap
                                    isProcessingBackground = false
                                }
                            }
                        }
                    }
                } else {
                    previewBitmap = null
                }
            } else if (selectedTool?.nameRes == R.string.portrait) {
                if (selectedPortraitOption != com.ai.vis.ui.components.PortraitOption.NONE) {
                    isProcessingPortrait = true
                    coroutineScope.launch(Dispatchers.IO) {
                        originalBitmap?.let { original ->
                            try {
                                val processPortraitUseCase = com.ai.vis.domain.usecase.ProcessPortraitUseCase(context)
                                processPortraitUseCase.initialize()
                                val result = processPortraitUseCase(
                                    bitmap = original,
                                    option = selectedPortraitOption,
                                    beautyIntensity = beautyIntensity,
                                    eyeIntensity = eyeIntensity,
                                    blurIntensity = blurFaceIntensity
                                )
                                withContext(Dispatchers.Main) {
                                    previewBitmap = result
                                    isProcessingPortrait = false
                                }
                                processPortraitUseCase.release()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    previewBitmap = originalBitmap
                                    isProcessingPortrait = false
                                }
                            }
                        }
                    }
                } else {
                    previewBitmap = null
                }
            } else {
                previewBitmap = null
            }
        }
    }
    
    fun performSavedStateUndo() {
        if (savedStatesUndoStack.isNotEmpty()) {
            originalBitmap?.let { current ->
                savedStatesRedoStack = savedStatesRedoStack + current
            }
            
            val previousBitmap = savedStatesUndoStack.last()
            savedStatesUndoStack = savedStatesUndoStack.dropLast(1)
            originalBitmap = previousBitmap
            previewBitmap = null
        }
    }
    
    fun performSavedStateRedo() {
        if (savedStatesRedoStack.isNotEmpty()) {
            originalBitmap?.let { current ->
                savedStatesUndoStack = savedStatesUndoStack + current
            }
            
            val nextBitmap = savedStatesRedoStack.last()
            savedStatesRedoStack = savedStatesRedoStack.dropLast(1)
            originalBitmap = nextBitmap
            previewBitmap = null
        }
    }
    
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            withContext(Dispatchers.IO) {
                originalBitmap = ImageProcessor.loadBitmap(context, imageUri)
            }
        }
    }
    
    LaunchedEffect(selectedTool) {
        if (selectedTool?.nameRes == R.string.text_tool) {
            showTextDialog = true
            dialogInputText = ""
        }
        
        if (selectedTool?.nameRes == R.string.portrait) {
            selectedPortraitOption = com.ai.vis.ui.components.PortraitOption.NONE
            previewBitmap = null
        }
        
        if (selectedTool != null && !isEditing) {
            originalBitmap?.let { bitmap ->
                stateBeforeEditing = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            }
        }
    }
    
    BackHandler(enabled = true) {
        when {
            isEditing -> {
                textItems = emptyList()
                drawPaths = emptyList()
                previewBitmap = null
                adjustmentValues = mapOf(
                    0 to 0f, 1 to 0f, 2 to 0f,
                    3 to 0f, 4 to 0f, 5 to 0f
                )
                showCropOverlay = false
                selectedCropRatio = null
                straightenAngle = 0f
                showTextDialog = false
                selectedTextId = null
                isEditing = false
                selectedTool = null
                undoStack = emptyList()
                redoStack = emptyList()
                stateBeforeEditing = null
            }
            selectedTool != null -> {
                selectedTool = null
                showCropOverlay = false
                selectedCropRatio = null
                straightenAngle = 0f
                showTextDialog = false
                selectedBackgroundOption = com.ai.vis.ui.components.BackgroundOption.NONE
                selectedBackgroundImage = null
                selectedPortraitOption = com.ai.vis.ui.components.PortraitOption.NONE
            }
            else -> {
                if (savedStatesUndoStack.isNotEmpty() || originalBitmap != null) {
                    showExitDialog = true
                } else {
                    onBackClick()
                }
            }
        }
    }
    
    fun savePhoto(location: com.ai.vis.ui.components.SaveLocation) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val finalBitmap = originalBitmap ?: return@launch
                val fileName = "AIVis_${System.currentTimeMillis()}.jpg"
                
                when (location) {
                    com.ai.vis.ui.components.SaveLocation.GALLERY_ONLY -> {
                        val uri = com.ai.vis.utils.PhotoSaver.saveToGallery(context, finalBitmap, fileName)
                        withContext(Dispatchers.Main) {
                            if (uri != null) {
                                saveSuccessMessage = context.getString(R.string.photo_saved_to_gallery)
                                showSaveSuccess = true
                                android.widget.Toast.makeText(context, saveSuccessMessage, android.widget.Toast.LENGTH_SHORT).show()
                                kotlinx.coroutines.delay(500)
                                onBackClick()
                            } else {
                                android.widget.Toast.makeText(context, context.getString(R.string.error_saving_photo), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    com.ai.vis.ui.components.SaveLocation.APP_ONLY -> {
                        val file = com.ai.vis.utils.PhotoSaver.saveToAppStorage(context, finalBitmap, fileName)
                        val thumbnail = com.ai.vis.utils.PhotoSaver.createThumbnail(context, finalBitmap, "thumb_$fileName")
                        
                        if (file != null) {
                            val editedPhoto = com.ai.vis.data.EditedPhoto(
                                filePath = file.absolutePath,
                                fileName = fileName,
                                timestamp = System.currentTimeMillis(),
                                thumbnailPath = thumbnail?.absolutePath,
                                width = finalBitmap.width,
                                height = finalBitmap.height,
                                sizeBytes = com.ai.vis.utils.PhotoSaver.getFileSize(file.absolutePath)
                            )
                            photoGalleryViewModel.insertPhoto(editedPhoto)
                            
                            withContext(Dispatchers.Main) {
                                saveSuccessMessage = context.getString(R.string.photo_saved_to_app)
                                showSaveSuccess = true
                                android.widget.Toast.makeText(context, saveSuccessMessage, android.widget.Toast.LENGTH_SHORT).show()
                                kotlinx.coroutines.delay(500)
                                onBackClick()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(context, context.getString(R.string.error_saving_photo), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    com.ai.vis.ui.components.SaveLocation.BOTH -> {
                        val uri = com.ai.vis.utils.PhotoSaver.saveToGallery(context, finalBitmap, fileName)
                        val file = com.ai.vis.utils.PhotoSaver.saveToAppStorage(context, finalBitmap, fileName)
                        val thumbnail = com.ai.vis.utils.PhotoSaver.createThumbnail(context, finalBitmap, "thumb_$fileName")
                        
                        if (file != null) {
                            val editedPhoto = com.ai.vis.data.EditedPhoto(
                                filePath = file.absolutePath,
                                fileName = fileName,
                                timestamp = System.currentTimeMillis(),
                                thumbnailPath = thumbnail?.absolutePath,
                                width = finalBitmap.width,
                                height = finalBitmap.height,
                                sizeBytes = com.ai.vis.utils.PhotoSaver.getFileSize(file.absolutePath)
                            )
                            photoGalleryViewModel.insertPhoto(editedPhoto)
                        }
                        
                        withContext(Dispatchers.Main) {
                            if (uri != null && file != null) {
                                saveSuccessMessage = context.getString(R.string.photo_saved_to_both)
                                showSaveSuccess = true
                                android.widget.Toast.makeText(context, saveSuccessMessage, android.widget.Toast.LENGTH_SHORT).show()
                                kotlinx.coroutines.delay(500)
                                onBackClick()
                            } else {
                                android.widget.Toast.makeText(context, context.getString(R.string.error_saving_photo), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "${context.getString(R.string.error_saving_photo)}: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    val editorTools = listOf(
        EditorTool(R.string.crop_rotate, R.drawable.ic_crop),
        EditorTool(R.string.adjust, R.drawable.ic_brightness),
        EditorTool(R.string.filters, R.drawable.ic_filters_main),
        EditorTool(R.string.stickers, R.drawable.ic_sticker),
        EditorTool(R.string.portrait, R.drawable.ic_ai),
        EditorTool(R.string.ai_styles, R.drawable.ic_filters),
        EditorTool(R.string.ai_background, R.drawable.ic_background),
        EditorTool(R.string.text_tool, R.drawable.ic_text),
        EditorTool(R.string.draw_tool, R.drawable.ic_paint)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (showCropOverlay) {
                        Text(
                            text = stringResource(id = R.string.crop_rotate),
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(R.font.font_title)),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { 
                                    if (isEditing) performUndo() else performSavedStateUndo()
                                },
                                enabled = if (isEditing) undoStack.isNotEmpty() else savedStatesUndoStack.isNotEmpty()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_undo),
                                    contentDescription = "Undo",
                                    tint = if ((isEditing && undoStack.isNotEmpty()) || (!isEditing && savedStatesUndoStack.isNotEmpty())) 
                                        MaterialTheme.colorScheme.onBackground 
                                    else 
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                )
                            }
                            
                            IconButton(
                                onClick = { 
                                    if (isEditing) performRedo() else performSavedStateRedo()
                                },
                                enabled = if (isEditing) redoStack.isNotEmpty() else savedStatesRedoStack.isNotEmpty()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_redo),
                                    contentDescription = "Redo",
                                    tint = if ((isEditing && redoStack.isNotEmpty()) || (!isEditing && savedStatesRedoStack.isNotEmpty())) 
                                        MaterialTheme.colorScheme.onBackground 
                                    else 
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditing) {
                            previewBitmap = null
                            adjustmentValues = mapOf(
                                0 to 0f, 1 to 0f, 2 to 0f,
                                3 to 0f, 4 to 0f, 5 to 0f
                            )
                            showCropOverlay = false
                            selectedCropRatio = null
                            cropFieldModified = false
                            straightenModified = false
                            showTextDialog = false
                            selectedTextId = null
                            textItems = emptyList()
                            drawPaths = emptyList()
                            stickerItems = emptyList()
                            isEditing = false
                            selectedTool = null  
                            selectedAIStyle = committedAIStyle
                            isToolPanelCollapsed = false
                            undoStack = emptyList()
                            redoStack = emptyList()
                            stateBeforeEditing = null
                        } else if (selectedTool != null) {
                            selectedTool = null
                            selectedAIStyle = committedAIStyle
                            previewBitmap = null
                            selectedTextId = null
                            showCropOverlay = false
                            selectedCropRatio = null
                            selectedBackgroundOption = com.ai.vis.ui.components.BackgroundOption.NONE
                            selectedBackgroundImage = null
                            selectedPortraitOption = com.ai.vis.ui.components.PortraitOption.NONE
                            cropFieldModified = false
                            straightenModified = false
                            isToolPanelCollapsed = false
                        } else {
                            if (savedStatesUndoStack.isNotEmpty() || originalBitmap != null) {
                                showExitDialog = true
                            } else {
                                onBackClick()
                            }
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = if (isEditing) R.drawable.ic_close else R.drawable.ic_arrow_back),
                            contentDescription = if (isEditing) stringResource(id = R.string.cancel) else stringResource(id = R.string.back)
                        )
                    }
                },
                actions = {
                    val isCropModified = selectedTool?.nameRes == R.string.crop_rotate && (cropFieldModified || straightenModified)
                    val isOtherToolEditing = selectedTool != null && selectedTool?.nameRes != R.string.crop_rotate && isEditing
                    val showCheckmark = isCropModified || isOtherToolEditing
                    
                    IconButton(onClick = {
                        if (isEditing) {
                            stateBeforeEditing?.let { beforeState ->
                                savedStatesUndoStack = savedStatesUndoStack + beforeState.copy(beforeState.config ?: Bitmap.Config.ARGB_8888, true)
                                savedStatesRedoStack = emptyList() 
                            }
                            
                            if (showCropOverlay && cropRect != null && imageBounds != null) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    originalBitmap?.let { bitmap ->
                                        originalBitmap = ImageProcessor.cropBitmapWithRect(
                                            bitmap = bitmap,
                                            cropRect = cropRect!!,
                                            imageBounds = imageBounds!!
                                        )
                                    }
                                }
                            }
                            
                            if (textItems.isNotEmpty() && selectedTool?.nameRes == R.string.text_tool && imageRectInBox != null && imageBounds != null) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    var resultBitmap = originalBitmap
                                    textItems.forEach { textItem ->
                                        val finalTextSize = textItem.style.size * textItem.scale
                                        val textSizePx = density.run { finalTextSize.sp.toPx() }
                                        val androidColor = android.graphics.Color.argb(
                                            (textItem.style.color.alpha * 255).toInt(),
                                            (textItem.style.color.red * 255).toInt(),
                                            (textItem.style.color.green * 255).toInt(),
                                            (textItem.style.color.blue * 255).toInt()
                                        )
                                        
                                        val drawAbs = Offset(
                                            imageBounds!!.left + (textItem.position.x - imageRectInBox!!.left),
                                            imageBounds!!.top + (textItem.position.y - imageRectInBox!!.top)
                                        )
                                        
                                        resultBitmap = resultBitmap?.let { bitmap ->
                                            ImageProcessor.drawTextOnBitmap(
                                                bitmap = bitmap,
                                                context = context,
                                                textContent = textItem.style.text,
                                                textSize = textSizePx,
                                                textColor = androidColor,
                                                textPosition = drawAbs,
                                                imageBounds = imageBounds!!,
                                                textAlign = android.graphics.Paint.Align.LEFT,
                                                isBold = textItem.style.weight == com.ai.vis.ui.components.TextWeight.BOLD,
                                                fontResourceId = textItem.style.fontFamily.fontRes,
                                                letterSpacing = textItem.style.letterSpacing,
                                                isItalic = textItem.style.isItalic,
                                                isUnderline = textItem.style.isUnderline,
                                                isStrikethrough = textItem.style.isStrikethrough,
                                                hasStroke = textItem.style.hasStroke,
                                                hasBackground = textItem.style.hasBackground,
                                                textOpacity = textItem.style.opacity,
                                                backgroundOpacity = textItem.style.backgroundOpacity,
                                                shadowRadius = textItem.style.shadowRadius,
                                                shadowOffsetX = textItem.style.shadowOffsetX,
                                                shadowOffsetY = textItem.style.shadowOffsetY,
                                                rotation = textItem.rotation
                                            )
                                        }
                                    }
                                    originalBitmap = resultBitmap
                                    textItems = emptyList()
                                    showTextDialog = false
                                    selectedTextId = null
                                }
                            }
                            
                            if (stickerItems.isNotEmpty() && selectedTool?.nameRes == R.string.stickers && imageRectInBox != null && imageBounds != null) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    var resultBitmap = originalBitmap
                                    stickerItems.forEach { stickerItem ->
                                        val finalEmojiSize = 60f * stickerItem.scale
                                        val emojiSizePx = density.run { finalEmojiSize.sp.toPx() }
                                        
                                        val drawAbs = Offset(
                                            imageBounds!!.left + (stickerItem.position.x - imageRectInBox!!.left),
                                            imageBounds!!.top + (stickerItem.position.y - imageRectInBox!!.top)
                                        )
                                        
                                        resultBitmap = resultBitmap?.let { bitmap ->
                                            ImageProcessor.drawStickerOnBitmap(
                                                bitmap = bitmap,
                                                emoji = stickerItem.emoji,
                                                position = drawAbs,
                                                emojiSizePx = emojiSizePx,
                                                rotation = stickerItem.rotation,
                                                opacity = stickerItem.opacity,
                                                imageBounds = imageBounds!!
                                            )
                                        }
                                    }
                                    originalBitmap = resultBitmap
                                    stickerItems = emptyList()
                                    selectedStickerId = null
                                }
                            }
                            
                            if (drawPaths.isNotEmpty() && selectedTool?.nameRes == R.string.draw_tool && imageBounds != null) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    originalBitmap = originalBitmap?.let { bitmap ->
                                        ImageProcessor.drawPathsOnBitmap(
                                            bitmap = bitmap,
                                            drawPaths = drawPaths,
                                            imageBounds = imageBounds!!
                                        )
                                    }
                                    drawPaths = emptyList()
                                }
                            }
                            
                            previewBitmap?.let {
                                originalBitmap = it
                                previewBitmap = null
                            }
                            if (selectedTool?.nameRes == R.string.ai_styles) {
                                committedAIStyle = selectedAIStyle
                            }
                            adjustmentValues = mapOf(
                                0 to 0f, 1 to 0f, 2 to 0f,
                                3 to 0f, 4 to 0f, 5 to 0f
                            )
                            showCropOverlay = false
                            selectedCropRatio = null
                            straightenAngle = 0f
                            cropFieldModified = false
                            straightenModified = false
                            showTextDialog = false
                            isEditing = false
                            selectedTool = null  
                            undoStack = emptyList()
                            redoStack = emptyList()
                            stateBeforeEditing = null
                        } else {
                            showSaveDialog = true
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = if (showCheckmark) R.drawable.ic_done else R.drawable.ic_save),
                            contentDescription = if (isEditing) stringResource(id = R.string.apply) else stringResource(id = R.string.save),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },

    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedTool, textItems, isEditing) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                if (selectedTool?.nameRes == R.string.text_tool && textItems.isNotEmpty()) {
                                    val tappedText = textItems.find { item ->
                                        val textSize = item.style.size * item.scale
                                        val bounds = Rect(
                                            item.position.x - 100f,
                                            item.position.y - textSize,
                                            item.position.x + 200f,
                                            item.position.y + textSize
                                        )
                                        bounds.contains(tapOffset)
                                    }
                                    
                                    if (tappedText != null) {
                                        selectedTextId = if (selectedTextId == tappedText.id) null else tappedText.id
                                        if (selectedTextId != null) {
                                            textStyle = textItems.find { it.id == selectedTextId }?.style ?: textStyle
                                        }
                                    }
                                } else if (selectedTool != null && selectedTool?.nameRes != R.string.text_tool) {
                                    if (!isToolPanelCollapsed) {
                                        isToolPanelCollapsed = true
                                    }
                                }
                            },
                            onDoubleTap = { tapOffset ->
                                if (selectedTool?.nameRes == R.string.text_tool && textItems.isNotEmpty()) {
                                    val tappedText = textItems.find { item ->
                                        val textSize = item.style.size * item.scale
                                        val bounds = Rect(
                                            item.position.x - 100f,
                                            item.position.y - textSize,
                                            item.position.x + 200f,
                                            item.position.y + textSize
                                        )
                                        bounds.contains(tapOffset)
                                    }
                                    
                                    if (tappedText != null) {
                                        selectedTextId = tappedText.id
                                        dialogInputText = tappedText.style.text
                                        showTextDialog = true
                                    }
                                }
                            },
                            onLongPress = { tapOffset ->
                                if (selectedTool?.nameRes == R.string.text_tool && textItems.isNotEmpty()) {
                                    val tappedText = textItems.find { item ->
                                        val textSize = item.style.size * item.scale
                                        val bounds = Rect(
                                            item.position.x - 100f,
                                            item.position.y - textSize,
                                            item.position.x + 200f,
                                            item.position.y + textSize
                                        )
                                        bounds.contains(tapOffset)
                                    }
                                    
                                    if (tappedText != null) {
                                        selectedTextId = tappedText.id
                                        dialogInputText = tappedText.style.text
                                        showTextDialog = true
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = density.run { bottomPanelsHeight.toDp() })
                            .onGloballyPositioned { coordinates ->
                                val size = coordinates.size.toSize()
                                val bitmap = displayBitmap ?: return@onGloballyPositioned
                                
                                val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                                val containerAspectRatio = size.width / size.height
                                
                                val (imageWidth, imageHeight) = if (imageAspectRatio > containerAspectRatio) {
                                    val w = size.width
                                    val h = w / imageAspectRatio
                                    w to h
                                } else {
                                    val h = size.height
                                    val w = h * imageAspectRatio
                                    w to h
                                }
                                
                                val leftInContainer = (size.width - imageWidth) / 2f
                                val topInContainer = (size.height - imageHeight) / 2f
                                
                                imageRectInBox = Rect(
                                    left = leftInContainer,
                                    top = topInContainer,
                                    right = leftInContainer + imageWidth,
                                    bottom = topInContainer + imageHeight
                                )
                                
                                imageBounds = imageRectInBox
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .then(
                                if (!showCropOverlay) Modifier.transformable(state = state)
                                else Modifier
                            ),
                        contentScale = ContentScale.Fit
                    )
                } else if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .then(
                                if (!showCropOverlay) Modifier.transformable(state = state)
                                else Modifier
                            ),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(id = R.string.tap_tool_hint),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                if (showCropOverlay && selectedCropRatio != null && displayBitmap != null && imageBounds != null) {
                    val cropRatioValue = when (selectedCropRatio) {
                        CropRatio.FREE -> null
                        CropRatio.RATIO_1_1 -> 1f
                        CropRatio.RATIO_4_3 -> 4f / 3f
                        CropRatio.RATIO_16_9 -> 16f / 9f
                        else -> null
                    }
                    
                    com.ai.vis.ui.components.CropOverlay(
                        cropRatio = cropRatioValue,
                        imageBounds = imageBounds,
                        scale = scale,
                        offset = offset,
                        onCropAreaChange = { rect ->
                            cropRect = rect
                            cropFieldModified = true  
                        }
                    )
                }
                
                if (selectedTool?.nameRes == R.string.draw_tool && imageBounds != null) {
                    com.ai.vis.ui.components.DrawingCanvas(
                        imageBounds = imageBounds,
                        drawPaths = drawPaths,
                        currentColor = drawColor,
                        currentStrokeWidth = drawStrokeWidth,
                        currentOpacity = drawOpacity,
                        currentSoftness = drawSoftness,
                        isEraserMode = isEraserMode,
                        currentShapeType = currentShapeType,
                        isShapeFilled = isShapeFilled,
                        onPathAdded = { newPath ->
                            drawPaths = drawPaths + newPath
                        },
                        onDrawingStarted = {
                            saveStateToUndo()
                            isEditing = true
                        }
                    )
                }
                
                textItems.forEach { textItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(textItem.id) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        selectedTextId = textItem.id
                                        dialogInputText = textItem.style.text
                                        showTextDialog = true
                                    }
                                )
                            }
                            .pointerInput(textItem.id) {
                                detectTransformGestures(
                                    onGesture = { _, pan, zoom, rotationChange ->
                                        if (!savedStateForTransform) {
                                            saveStateToUndo()
                                            savedStateForTransform = true
                                        }
                                        
                                        textItems = textItems.map {
                                            if (it.id == textItem.id) {
                                                val newScale = (it.scale * zoom).coerceIn(0.5f, 5f)
                                                val newPosition = Offset(
                                                    x = it.position.x + pan.x,
                                                    y = it.position.y + pan.y
                                                )
                                                val newRotation = it.rotation + rotationChange
                                                it.copy(scale = newScale, position = newPosition, rotation = newRotation)
                                            } else it
                                        }
                                    }
                                )
                            }
                            .pointerInput(textItem.id) {
                                detectTapGestures(
                                    onPress = {
                                        savedStateForTransform = false
                                        tryAwaitRelease()
                                        savedStateForTransform = false
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = textItem.style.text,
                            fontSize = (textItem.style.size * textItem.scale).sp,
                            fontFamily = FontFamily(Font(textItem.style.fontFamily.fontRes)),
                            color = textItem.style.color.copy(alpha = textItem.style.opacity),
                            fontWeight = when (textItem.style.weight) {
                                com.ai.vis.ui.components.TextWeight.LIGHT -> FontWeight.Light
                                com.ai.vis.ui.components.TextWeight.NORMAL -> FontWeight.Normal
                                com.ai.vis.ui.components.TextWeight.BOLD -> FontWeight.Bold
                            },
                            fontStyle = if (textItem.style.isItalic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = when {
                                textItem.style.isUnderline && textItem.style.isStrikethrough -> 
                                    androidx.compose.ui.text.style.TextDecoration.combine(
                                        listOf(
                                            androidx.compose.ui.text.style.TextDecoration.Underline,
                                            androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        )
                                    )
                                textItem.style.isUnderline -> androidx.compose.ui.text.style.TextDecoration.Underline
                                textItem.style.isStrikethrough -> androidx.compose.ui.text.style.TextDecoration.LineThrough
                                else -> androidx.compose.ui.text.style.TextDecoration.None
                            },
                            letterSpacing = (textItem.style.letterSpacing * 0.1f).sp,
                            style = if (textItem.style.shadowRadius > 0) {
                                androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        offset = androidx.compose.ui.geometry.Offset(
                                            textItem.style.shadowOffsetX,
                                            textItem.style.shadowOffsetY
                                        ),
                                        blurRadius = textItem.style.shadowRadius
                                    )
                                )
                            } else {
                                androidx.compose.ui.text.TextStyle()
                            },
                            modifier = Modifier
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        textItem.position.x.toInt(),
                                        textItem.position.y.toInt()
                                    )
                                }
                                .graphicsLayer {
                                    rotationZ = textItem.rotation
                                }
                                .background(
                                    color = if (textItem.style.hasBackground)
                                        MaterialTheme.colorScheme.surface.copy(alpha = textItem.style.backgroundOpacity)
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .border(
                                    width = if (selectedTextId == textItem.id) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
                
                stickerItems.forEach { stickerItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(stickerItem.id) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        saveStateToUndo()
                                        stickerItems = stickerItems.filter { it.id != stickerItem.id }
                                        if (selectedStickerId == stickerItem.id) {
                                            selectedStickerId = null
                                        }
                                    }
                                )
                            }
                            .pointerInput(stickerItem.id) {
                                detectTransformGestures(
                                    onGesture = { _, pan, zoom, rotationChange ->
                                        if (!savedStateForTransform) {
                                            saveStateToUndo()
                                            savedStateForTransform = true
                                        }
                                        
                                        stickerItems = stickerItems.map {
                                            if (it.id == stickerItem.id) {
                                                val newScale = (it.scale * zoom).coerceIn(0.5f, 5f)
                                                val newPosition = Offset(
                                                    x = it.position.x + pan.x,
                                                    y = it.position.y + pan.y
                                                )
                                                val newRotation = it.rotation + rotationChange
                                                it.copy(scale = newScale, position = newPosition, rotation = newRotation)
                                            } else it
                                        }
                                    }
                                )
                            }
                            .pointerInput(stickerItem.id) {
                                detectTapGestures(
                                    onPress = {
                                        savedStateForTransform = false
                                        tryAwaitRelease()
                                        savedStateForTransform = false
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = stickerItem.emoji,
                            fontSize = (60 * stickerItem.scale).sp,
                            modifier = Modifier
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        stickerItem.position.x.toInt(),
                                        stickerItem.position.y.toInt()
                                    )
                                }
                                .graphicsLayer {
                                    rotationZ = stickerItem.rotation
                                    alpha = stickerItem.opacity
                                }
                                .border(
                                    width = if (selectedStickerId == stickerItem.id) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp)
                        )
                    }
                }
            }

            com.ai.vis.ui.components.TextInputDialog(
                visible = showTextDialog,
                initialText = dialogInputText,
                isEditMode = selectedTextId != null && dialogInputText.isNotEmpty(),
                onDismiss = {
                    showTextDialog = false
                    if (dialogInputText.isEmpty()) {
                        selectedTool = null
                    }
                    dialogInputText = ""
                },
                onConfirm = { text ->
                    saveStateToUndo()
                    
                    if (selectedTextId != null && dialogInputText.isNotEmpty()) {
                        textItems = textItems.map {
                            if (it.id == selectedTextId) {
                                it.copy(style = it.style.copy(text = text))
                            } else it
                        }
                        textStyle = textStyle.copy(text = text)
                    } else {
                        val centerPos = imageRectInBox?.let {
                            Offset(
                                x = it.left + it.width / 2f,
                                y = it.top + it.height / 2f
                            )
                        } ?: Offset(500f, 500f)
                        
                        val newId = nextTextId
                        nextTextId++
                        val newItem = TextItem(
                            id = newId,
                            position = centerPos,
                            scale = 1f,
                            style = textStyle.copy(text = text)
                        )
                        textItems = textItems + newItem
                        selectedTextId = newId
                    }
                    isEditing = true
                    showTextDialog = false
                    dialogInputText = ""
                }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = selectedTool != null && !isToolPanelCollapsed,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        when (selectedTool?.nameRes) {
                            R.string.crop_rotate -> {
                                com.ai.vis.ui.components.CropRotatePanel(
                                    currentStraightenAngle = straightenAngle,
                                    onCropRatioSelected = { ratio ->
                                        saveStateToUndo()
                                        selectedCropRatio = ratio
                                        showCropOverlay = true
                                        cropFieldModified = false  
                                        isEditing = true
                                        scale = 1f
                                        offset = Offset.Zero
                                    },
                                    onStraightenAngleChange = { angle ->
                                        if (straightenAngle == 0f) {
                                            originalBitmap?.let { bitmap ->
                                                savedStatesUndoStack = savedStatesUndoStack + bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                                                savedStatesRedoStack = emptyList()
                                            }
                                        }
                                        straightenAngle = angle
                                        straightenModified = true  
                                        isEditing = true
                                        
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { bitmap ->
                                                val basebitmap = savedStatesUndoStack.lastOrNull() ?: bitmap
                                                previewBitmap = ImageProcessor.rotateBitmap(basebitmap, angle)
                                            }
                                        }
                                    },
                                    onRotateLeft = {
                                        originalBitmap?.let { bitmap ->
                                            savedStatesUndoStack = savedStatesUndoStack + bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                                            savedStatesRedoStack = emptyList()
                                        }
                                        
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { bitmap ->
                                                originalBitmap = ImageProcessor.rotateBitmap(bitmap, -90f)
                                                previewBitmap = null
                                            }
                                        }
                                    },
                                    onRotateRight = {
                                        originalBitmap?.let { bitmap ->
                                            savedStatesUndoStack = savedStatesUndoStack + bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                                            savedStatesRedoStack = emptyList()
                                        }
                                        
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { bitmap ->
                                                originalBitmap = ImageProcessor.rotateBitmap(bitmap, 90f)
                                                previewBitmap = null
                                            }
                                        }
                                    },
                                    onFlipHorizontal = {
                                        originalBitmap?.let { bitmap ->
                                            savedStatesUndoStack = savedStatesUndoStack + bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                                            savedStatesRedoStack = emptyList()
                                        }
                                        
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { bitmap ->
                                                originalBitmap = ImageProcessor.flipBitmap(bitmap, horizontal = true)
                                                previewBitmap = null
                                            }
                                        }
                                    },
                                    onFlipVertical = {
                                        originalBitmap?.let { bitmap ->
                                            savedStatesUndoStack = savedStatesUndoStack + bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                                            savedStatesRedoStack = emptyList()
                                        }
                                        
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { bitmap ->
                                                originalBitmap = ImageProcessor.flipBitmap(bitmap, horizontal = false)
                                                previewBitmap = null
                                            }
                                        }
                                    }
                                )
                            }
                            R.string.adjust -> {
                                com.ai.vis.ui.components.AdjustPanel(
                                    adjustmentValues = adjustmentValues,
                                    onValueChangeStarted = { index ->
                                        saveStateToUndo()
                                    },
                                    onValueChange = { index, value ->
                                        isEditing = true
                                        adjustmentValues = adjustmentValues.toMutableMap().apply {
                                            this[index] = value
                                        }
                                        
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { original ->
                                                var result = original
                                                
                                                adjustmentValues[0]?.let { brightness ->
                                                    if (brightness != 0f) result = ImageProcessor.adjustBrightness(result, brightness)
                                                }
                                                adjustmentValues[1]?.let { contrast ->
                                                    if (contrast != 0f) result = ImageProcessor.adjustContrast(result, contrast)
                                                }
                                                adjustmentValues[2]?.let { saturation ->
                                                    if (saturation != 0f) result = ImageProcessor.adjustSaturation(result, saturation)
                                                }
                                                adjustmentValues[3]?.let { sharpness ->
                                                    if (sharpness != 0f) result = ImageProcessor.adjustSharpness(result, sharpness)
                                                }
                                                adjustmentValues[4]?.let { temperature ->
                                                    if (temperature != 0f) result = ImageProcessor.adjustTemperature(result, temperature)
                                                }
                                                adjustmentValues[5]?.let { tint ->
                                                    if (tint != 0f) result = ImageProcessor.adjustTint(result, tint)
                                                }
                                                
                                                previewBitmap = result
                                            }
                                        }
                                    },
                                    onValueChangeFinished = { index ->
                                    }
                                )
                            }
                            R.string.filters -> {
                                com.ai.vis.ui.components.FilterPanel(
                                    currentFilter = currentFilter,
                                    filterIntensity = filterIntensity,
                                    onFilterChange = { filter ->
                                        saveStateToUndo()
                                        currentFilter = filter
                                        isEditing = true
                                        
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { original ->
                                                val result = when (filter) {
                                                    com.ai.vis.ui.components.FilterType.NONE -> original
                                                    com.ai.vis.ui.components.FilterType.BW -> ImageProcessor.applyBWFilter(original, filterIntensity)
                                                    com.ai.vis.ui.components.FilterType.SEPIA -> ImageProcessor.applySepiaFilter(original, filterIntensity)
                                                    com.ai.vis.ui.components.FilterType.VINTAGE -> ImageProcessor.applyVintageFilter(original, filterIntensity)
                                                    com.ai.vis.ui.components.FilterType.COOL -> ImageProcessor.applyCoolFilter(original, filterIntensity)
                                                    com.ai.vis.ui.components.FilterType.WARM -> ImageProcessor.applyWarmFilter(original, filterIntensity)
                                                    com.ai.vis.ui.components.FilterType.GRAYSCALE -> ImageProcessor.applyGrayscaleFilter(original, filterIntensity)
                                                    com.ai.vis.ui.components.FilterType.INVERT -> ImageProcessor.applyInvertFilter(original, filterIntensity)
                                                }
                                                previewBitmap = result
                                            }
                                        }
                                    },
                                    onIntensityChangeStarted = {
                                        saveStateToUndo()
                                    },
                                    onIntensityChange = { intensity ->
                                        filterIntensity = intensity
                                        
                                        if (currentFilter != com.ai.vis.ui.components.FilterType.NONE) {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                originalBitmap?.let { original ->
                                                    val result = when (currentFilter) {
                                                        com.ai.vis.ui.components.FilterType.NONE -> original
                                                        com.ai.vis.ui.components.FilterType.BW -> ImageProcessor.applyBWFilter(original, intensity)
                                                        com.ai.vis.ui.components.FilterType.SEPIA -> ImageProcessor.applySepiaFilter(original, intensity)
                                                        com.ai.vis.ui.components.FilterType.VINTAGE -> ImageProcessor.applyVintageFilter(original, intensity)
                                                        com.ai.vis.ui.components.FilterType.COOL -> ImageProcessor.applyCoolFilter(original, intensity)
                                                        com.ai.vis.ui.components.FilterType.WARM -> ImageProcessor.applyWarmFilter(original, intensity)
                                                        com.ai.vis.ui.components.FilterType.GRAYSCALE -> ImageProcessor.applyGrayscaleFilter(original, intensity)
                                                        com.ai.vis.ui.components.FilterType.INVERT -> ImageProcessor.applyInvertFilter(original, intensity)
                                                    }
                                                    previewBitmap = result
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            R.string.stickers -> {
                                com.ai.vis.ui.components.StickerPanel(
                                    currentSize = stickerSize,
                                    currentOpacity = stickerOpacity,
                                    onSizeChange = { size ->
                                        stickerSize = size
                                        if (selectedStickerId != null) {
                                            saveStateToUndo()
                                            stickerItems = stickerItems.map {
                                                if (it.id == selectedStickerId) it.copy(scale = size) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onOpacityChange = { opacity ->
                                        stickerOpacity = opacity
                                        selectedStickerId?.let { id ->
                                            stickerItems = stickerItems.map { sticker ->
                                                if (sticker.id == id) {
                                                    sticker.copy(opacity = opacity)
                                                } else {
                                                    sticker
                                                }
                                            }
                                        }
                                    },
                                    onStickerSelected = { emoji ->
                                        saveStateToUndo()
                                        
                                        val centerPos = imageBounds?.center ?: Offset(500f, 500f)
                                        val newSticker = StickerItem(
                                            id = nextStickerId++,
                                            emoji = emoji,
                                            position = centerPos,
                                            scale = stickerSize,
                                            rotation = 0f,
                                            opacity = stickerOpacity
                                        )
                                        stickerItems = stickerItems + newSticker
                                        selectedStickerId = newSticker.id
                                        isEditing = true
                                    }
                                )
                            }
                            R.string.text_tool -> {
                                com.ai.vis.ui.components.TextPanel(
                                    textStyle = textStyle,
                                    onTextChange = { text ->
                                        textStyle = textStyle.copy(text = text)
                                        isEditing = text.isNotEmpty()
                                    },
                                    onSizeChange = { size ->
                                        saveStateToUndo()
                                        textStyle = textStyle.copy(size = size)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(size = size)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onFontChange = { fontFamily ->
                                        saveStateToUndo()
                                        textStyle = textStyle.copy(fontFamily = fontFamily)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(fontFamily = fontFamily)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onColorChange = { color ->
                                        saveStateToUndo()
                                        textStyle = textStyle.copy(color = color)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(color = color)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onAlignmentChange = { alignment ->
                                        saveStateToUndo()
                                        textStyle = textStyle.copy(alignment = alignment)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(alignment = alignment)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onWeightChange = { weight ->
                                        saveStateToUndo()
                                        val nextWeight = when (textStyle.weight) {
                                            com.ai.vis.ui.components.TextWeight.NORMAL -> com.ai.vis.ui.components.TextWeight.BOLD
                                            com.ai.vis.ui.components.TextWeight.BOLD -> com.ai.vis.ui.components.TextWeight.LIGHT
                                            com.ai.vis.ui.components.TextWeight.LIGHT -> com.ai.vis.ui.components.TextWeight.NORMAL
                                        }
                                        textStyle = textStyle.copy(weight = nextWeight)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(weight = nextWeight)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onLetterSpacingChange = { spacing ->
                                        textStyle = textStyle.copy(letterSpacing = spacing)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(letterSpacing = spacing)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onItalicToggle = { isItalic ->
                                        saveStateToUndo()
                                        textStyle = textStyle.copy(isItalic = isItalic)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(isItalic = isItalic)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onUnderlineToggle = { isUnderline ->
                                        saveStateToUndo()
                                        textStyle = textStyle.copy(isUnderline = isUnderline)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(isUnderline = isUnderline)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onStrikethroughToggle = { isStrikethrough ->
                                        saveStateToUndo()
                                        textStyle = textStyle.copy(isStrikethrough = isStrikethrough)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(isStrikethrough = isStrikethrough)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onStrokeToggle = { hasStroke ->
                                        saveStateToUndo()
                                        textStyle = textStyle.copy(hasStroke = hasStroke)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(hasStroke = hasStroke)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onBackgroundToggle = { hasBackground ->
                                        saveStateToUndo()
                                        val newBackground = !textStyle.hasBackground
                                        textStyle = textStyle.copy(hasBackground = newBackground)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(hasBackground = newBackground)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onOpacityChange = { opacity ->
                                        textStyle = textStyle.copy(opacity = opacity)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(opacity = opacity)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onBackgroundOpacityChange = { backgroundOpacity ->
                                        textStyle = textStyle.copy(backgroundOpacity = backgroundOpacity)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(backgroundOpacity = backgroundOpacity)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onShadowChange = { radius, offsetX, offsetY ->
                                        textStyle = textStyle.copy(
                                            shadowRadius = radius,
                                            shadowOffsetX = offsetX,
                                            shadowOffsetY = offsetY
                                        )
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(
                                                    style = it.style.copy(
                                                        shadowRadius = radius,
                                                        shadowOffsetX = offsetX,
                                                        shadowOffsetY = offsetY
                                                    )
                                                ) else it
                                            }
                                            isEditing = true
                                        }
                                    }
                                )
                            }
                            R.string.draw_tool -> {
                                com.ai.vis.ui.components.DrawPanel(
                                    currentColor = drawColor,
                                    currentSize = drawStrokeWidth,
                                    currentOpacity = drawOpacity,
                                    currentSoftness = drawSoftness,
                                    isEraserMode = isEraserMode,
                                    currentShapeType = currentShapeType,
                                    isShapeFilled = isShapeFilled,
                                    onColorChange = { color ->
                                        drawColor = color
                                    },
                                    onSizeChange = { size ->
                                        drawStrokeWidth = size
                                    },
                                    onOpacityChange = { opacity ->
                                        drawOpacity = opacity
                                    },
                                    onSoftnessChange = { softness ->
                                        drawSoftness = softness
                                    },
                                    onEraserToggle = { isEraser ->
                                        isEraserMode = isEraser
                                    },
                                    onShapeTypeChange = { shapeType ->
                                        currentShapeType = shapeType
                                    },
                                    onShapeFillToggle = { isFilled ->
                                        isShapeFilled = isFilled
                                    },
                                    onErase = {
                                        if (drawPaths.isNotEmpty()) {
                                            saveStateToUndo()
                                            drawPaths = drawPaths.dropLast(1)
                                        }
                                    }
                                )
                            }
                            R.string.ai_background -> {
                                com.ai.vis.ui.components.BackgroundPanel(
                                    selectedOption = selectedBackgroundOption,
                                    isProcessing = isProcessingBackground,
                                    blurRadius = blurRadius,
                                    onBlurRadiusChange = { newRadius ->
                                        blurRadius = newRadius
                                        if (selectedBackgroundOption == com.ai.vis.ui.components.BackgroundOption.BLUR && !isProcessingBackground) {
                                            isProcessingBackground = true
                                            originalBitmap?.let { original ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        val processBackgroundUseCase = com.ai.vis.domain.usecase.ProcessBackgroundUseCase(context)
                                                        processBackgroundUseCase.initialize()
                                                        val result = processBackgroundUseCase(
                                                            bitmap = original,
                                                            option = com.ai.vis.ui.components.BackgroundOption.BLUR,
                                                            blurRadius = newRadius.toInt()
                                                        )
                                                        withContext(Dispatchers.Main) {
                                                            previewBitmap = result
                                                            isProcessingBackground = false
                                                        }
                                                        processBackgroundUseCase.release()
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            isProcessingBackground = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    selectedBackgroundImage = selectedBackgroundImage,
                                    onSelectBackgroundImage = {
                                        backgroundImageLauncher.launch("image/*")
                                    },
                                    onOptionSelected = { option ->
                                        if (!isProcessingBackground) {
                                            selectedBackgroundOption = option
                                            saveStateToUndo()
                                            isEditing = true
                                            isProcessingBackground = true
                                            
                                            originalBitmap?.let { original ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        val processBackgroundUseCase = com.ai.vis.domain.usecase.ProcessBackgroundUseCase(context)
                                                        
                                                        processBackgroundUseCase.initialize()
                                                        
                                                        val result = processBackgroundUseCase(
                                                            bitmap = original,
                                                            option = option,
                                                            blurRadius = blurRadius.toInt(),
                                                            backgroundImage = selectedBackgroundImage
                                                        )
                                                        
                                                        withContext(Dispatchers.Main) {
                                                            previewBitmap = result
                                                            isProcessingBackground = false
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Background processed successfully!",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                        
                                                        processBackgroundUseCase.release()
                                                    } catch (e: java.io.FileNotFoundException) {
                                                        withContext(Dispatchers.Main) {
                                                            isProcessingBackground = false
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Model file not found! Please add selfie_segmentation.tflite to assets/models/",
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            isProcessingBackground = false
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Error: ${e.message}",
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                                
                                LaunchedEffect(selectedBackgroundImage) {
                                    if (selectedBackgroundOption == com.ai.vis.ui.components.BackgroundOption.REPLACE && 
                                        selectedBackgroundImage != null && 
                                        !isProcessingBackground) {
                                        isProcessingBackground = true
                                        originalBitmap?.let { original ->
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    val processBackgroundUseCase = com.ai.vis.domain.usecase.ProcessBackgroundUseCase(context)
                                                    processBackgroundUseCase.initialize()
                                                    val result = processBackgroundUseCase(
                                                        bitmap = original,
                                                        option = com.ai.vis.ui.components.BackgroundOption.REPLACE,
                                                        backgroundImage = selectedBackgroundImage
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        previewBitmap = result
                                                        isProcessingBackground = false
                                                    }
                                                    processBackgroundUseCase.release()
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        isProcessingBackground = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            R.string.ai_styles -> {
                                com.ai.vis.ui.components.AIStylesPanel(
                                    selectedStyle = selectedAIStyle,
                                    isProcessing = isApplyingAIStyle,
                                    onStyleSelected = { style ->
                                        if (!isApplyingAIStyle) {
                                            if (style == com.ai.vis.domain.model.AIStyle.NONE) {
                                                selectedAIStyle = style
                                                previewBitmap = null
                                                isEditing = false
                                            } else {
                                                originalBitmap?.let { original ->
                                                    saveStateToUndo()
                                                    selectedAIStyle = style
                                                    isApplyingAIStyle = true
                                                    isEditing = true

                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        try {
                                                            val applyAIStyleUseCase = com.ai.vis.domain.usecase.ApplyAIStyleUseCase(context)
                                                            val result = applyAIStyleUseCase(original, style)
                                                            withContext(Dispatchers.Main) {
                                                                previewBitmap = result
                                                            }
                                                            applyAIStyleUseCase.release()
                                                        } catch (e: Exception) {
                                                            withContext(Dispatchers.Main) {
                                                                previewBitmap = originalBitmap
                                                            }
                                                        } finally {
                                                            withContext(Dispatchers.Main) {
                                                                isApplyingAIStyle = false
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            R.string.portrait -> {
                                com.ai.vis.ui.components.PortraitPanel(
                                    selectedOption = selectedPortraitOption,
                                    isProcessing = isProcessingPortrait,
                                    beautyIntensity = beautyIntensity,
                                    onBeautyIntensityChange = { intensity ->
                                        beautyIntensity = intensity
                                        if (selectedPortraitOption == com.ai.vis.ui.components.PortraitOption.BEAUTY_MODE && !isProcessingPortrait) {
                                            isProcessingPortrait = true
                                            originalBitmap?.let { original ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        val processPortraitUseCase = com.ai.vis.domain.usecase.ProcessPortraitUseCase(context)
                                                        processPortraitUseCase.initialize()
                                                        val result = processPortraitUseCase(
                                                            bitmap = original,
                                                            option = com.ai.vis.ui.components.PortraitOption.BEAUTY_MODE,
                                                            beautyIntensity = intensity
                                                        )
                                                        withContext(Dispatchers.Main) {
                                                            previewBitmap = result
                                                            isProcessingPortrait = false
                                                        }
                                                        processPortraitUseCase.release()
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            isProcessingPortrait = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    eyeIntensity = eyeIntensity,
                                    onEyeIntensityChange = { intensity ->
                                        eyeIntensity = intensity
                                        if (selectedPortraitOption == com.ai.vis.ui.components.PortraitOption.EYE_ENHANCEMENT && !isProcessingPortrait) {
                                            isProcessingPortrait = true
                                            originalBitmap?.let { original ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        val processPortraitUseCase = com.ai.vis.domain.usecase.ProcessPortraitUseCase(context)
                                                        processPortraitUseCase.initialize()
                                                        val result = processPortraitUseCase(
                                                            bitmap = original,
                                                            option = com.ai.vis.ui.components.PortraitOption.EYE_ENHANCEMENT,
                                                            eyeIntensity = intensity
                                                        )
                                                        withContext(Dispatchers.Main) {
                                                            previewBitmap = result
                                                            isProcessingPortrait = false
                                                        }
                                                        processPortraitUseCase.release()
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            isProcessingPortrait = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    blurIntensity = blurFaceIntensity,
                                    onBlurIntensityChange = { intensity ->
                                        blurFaceIntensity = intensity
                                        if (selectedPortraitOption == com.ai.vis.ui.components.PortraitOption.FACE_BLUR && !isProcessingPortrait) {
                                            isProcessingPortrait = true
                                            originalBitmap?.let { original ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        val processPortraitUseCase = com.ai.vis.domain.usecase.ProcessPortraitUseCase(context)
                                                        processPortraitUseCase.initialize()
                                                        val result = processPortraitUseCase(
                                                            bitmap = original,
                                                            option = com.ai.vis.ui.components.PortraitOption.FACE_BLUR,
                                                            blurIntensity = intensity
                                                        )
                                                        withContext(Dispatchers.Main) {
                                                            previewBitmap = result
                                                            isProcessingPortrait = false
                                                        }
                                                        processPortraitUseCase.release()
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            isProcessingPortrait = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onOptionSelected = { option ->
                                        if (!isProcessingPortrait) {
                                            selectedPortraitOption = option
                                            if (option != com.ai.vis.ui.components.PortraitOption.NONE) {
                                                saveStateToUndo()
                                                isEditing = true
                                                isProcessingPortrait = true
                                                
                                                originalBitmap?.let { original ->
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        try {
                                                            val processPortraitUseCase = com.ai.vis.domain.usecase.ProcessPortraitUseCase(context)
                                                            processPortraitUseCase.initialize()
                                                            val result = processPortraitUseCase(
                                                                bitmap = original,
                                                                option = option,
                                                                beautyIntensity = beautyIntensity,
                                                                eyeIntensity = eyeIntensity,
                                                                blurIntensity = blurFaceIntensity
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                previewBitmap = result
                                                                isProcessingPortrait = false
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "Portrait effect applied!",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                            processPortraitUseCase.release()
                                                        } catch (e: Exception) {
                                                            withContext(Dispatchers.Main) {
                                                                isProcessingPortrait = false
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "Error: ${e.message}",
                                                                    android.widget.Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                previewBitmap = null
                                                isEditing = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            bottomPanelsHeight = coordinates.size.height.toFloat()
                        }
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                ) {
                    when (selectedTool?.nameRes) {
                        R.string.text_tool, R.string.adjust, R.string.draw_tool, R.string.filters, R.string.stickers, R.string.ai_styles, R.string.ai_background, R.string.portrait -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { 
                                        isToolPanelCollapsed = !isToolPanelCollapsed
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (isToolPanelCollapsed) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                                        ),
                                        contentDescription = if (isToolPanelCollapsed) "Розгорнути" else "Згорнути",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                Text(
                                    text = stringResource(id = selectedTool?.nameRes ?: R.string.editing),
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Box(modifier = Modifier.size(48.dp))
                            }
                        }
                        else -> {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(editorTools) { tool ->
                                    EditorToolItem(
                                        tool = tool,
                                        isSelected = selectedTool == tool,
                                        onClick = { 
                                            val newTool = if (selectedTool == tool) null else tool
                                            selectedTool = newTool
                                            if (newTool != null) {
                                                isToolPanelCollapsed = false
                                            }
                                            if (tool.nameRes == R.string.text_tool && selectedTool != tool) {
                                                showTextDialog = false
                                                selectedTextId = null
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        com.ai.vis.ui.components.ExitConfirmDialog(
            visible = showExitDialog,
            onDismiss = { showExitDialog = false },
            onConfirm = { 
                showExitDialog = false
                onBackClick()
            }
        )
        
        com.ai.vis.ui.components.SaveOptionsDialog(
            visible = showSaveDialog,
            onDismiss = { showSaveDialog = false },
            onSaveLocationSelected = { location ->
                showSaveDialog = false
                savePhoto(location)
            }
        )
    }
}

@Composable
fun EditorToolItem(
    tool: EditorTool,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(80.dp)
            .height(80.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                com.ai.vis.ui.theme.SelectionColor()
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = tool.iconRes),
                    contentDescription = null,
                    tint = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(id = tool.nameRes),
                    fontSize = 10.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 12.sp,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    minLines = 2
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PhotoEditorScreenPreview() {
    AIVisTheme {
        PhotoEditorScreen(
            imageUri = null,
            onBackClick = {}
        )
    }
}
