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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// Text item data class for undo/redo support
data class TextItem(
    val id: Int,
    var position: Offset,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var style: com.ai.vis.ui.components.TextStyle
)

// Data class to store editor state for undo/redo
data class EditorState(
    val bitmap: Bitmap,
    val textItems: List<TextItem> = emptyList(),
    val drawPaths: List<com.ai.vis.ui.components.DrawPath> = emptyList(),
    val adjustmentValues: Map<Int, Float> = mapOf(
        0 to 0f, 1 to 0f, 2 to 0f,
        3 to 0f, 4 to 0f, 5 to 0f
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    imageUri: Uri?,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Image states - originalBitmap is the current saved state
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) } // For preview during editing
    var displayBitmap = previewBitmap ?: originalBitmap // What to show
    
    // Undo/Redo stacks for editing session
    var undoStack by remember { mutableStateOf<List<EditorState>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<EditorState>>(emptyList()) }
    
    // Undo/Redo stacks for main screen (saved states after OK)
    var savedStatesUndoStack by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var savedStatesRedoStack by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    
    // Transform state for zoom and pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += offsetChange
    }
    
    // Selected tool state
    var selectedTool by remember { mutableStateOf<EditorTool?>(null) }
    
    // Track if user is editing (to show Apply/Cancel buttons)
    var isEditing by remember { mutableStateOf(false) }
    
    // Crop state - don't select any ratio by default
    var selectedCropRatio by remember { mutableStateOf<CropRatio?>(null) }
    var showCropOverlay by remember { mutableStateOf(false) }
    var cropRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    // Image bounds for crop overlay - recalculate only when bitmap changes
    var imageBounds by remember(originalBitmap) { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    // Density for sp<->px conversions (capture once in composable scope)
    val density = LocalDensity.current
    
    // Track bottom panels height for padding
    var bottomPanelsHeight by remember { mutableFloatStateOf(0f) }
    
    // Adjustment values state - need to be mutableState for slider sync
    var adjustmentValues by remember { mutableStateOf(mapOf(
        0 to 0f, // brightness
        1 to 0f, // contrast
        2 to 0f, // saturation
        3 to 0f, // sharpness
        4 to 0f, // temperature
        5 to 0f  // tint
    )) }
    
    // Текстові елементи з масштабуванням 📝
    var textItems by remember { mutableStateOf<List<TextItem>>(emptyList()) }
    var selectedTextId by remember { mutableStateOf<Int?>(null) }
    var nextTextId by remember { mutableStateOf(0) }
    var showTextDialog by remember { mutableStateOf(false) }
    var dialogInputText by remember { mutableStateOf("") }
    var textStyle by remember { mutableStateOf(com.ai.vis.ui.components.TextStyle()) }
    
    // Track if we saved state for current text transformation
    var savedStateForTransform by remember { mutableStateOf(false) }
    
    // Drawing state 🎨
    var drawPaths by remember { mutableStateOf<List<com.ai.vis.ui.components.DrawPath>>(emptyList()) }
    var drawColor by remember { mutableStateOf(Color.Black) }
    var drawStrokeWidth by remember { mutableFloatStateOf(10f) }
    var drawOpacity by remember { mutableFloatStateOf(1f) }
    var drawSoftness by remember { mutableFloatStateOf(0f) }
    var isEraserMode by remember { mutableStateOf(false) }
    
    // Розмір і позиція Image в Box (для збереження тексту на bitmap)
    var imageRectInBox by remember { mutableStateOf<Rect?>(null) }
    
    // Helper function to save current state to undo stack
    fun saveStateToUndo() {
        originalBitmap?.let { bitmap ->
            val currentState = EditorState(
                bitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
                textItems = textItems.map { it.copy() },
                drawPaths = drawPaths.map { it.copy() },
                adjustmentValues = adjustmentValues.toMap()
            )
            undoStack = undoStack + currentState
            redoStack = emptyList() // Clear redo stack when new action is performed
        }
    }
    
    // Helper function to perform undo
    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val currentState = originalBitmap?.let { bitmap ->
                EditorState(
                    bitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
                    textItems = textItems.map { it.copy() },
                    drawPaths = drawPaths.map { it.copy() },
                    adjustmentValues = adjustmentValues.toMap()
                )
            }
            
            val previousState = undoStack.last()
            undoStack = undoStack.dropLast(1)
            
            currentState?.let { redoStack = redoStack + it }
            
            originalBitmap = previousState.bitmap
            textItems = previousState.textItems
            drawPaths = previousState.drawPaths
            adjustmentValues = previousState.adjustmentValues
            
            // If we're in adjustment mode, re-apply the restored adjustment values to preview
            if (selectedTool?.nameRes == R.string.adjust) {
                coroutineScope.launch(Dispatchers.IO) {
                    originalBitmap?.let { original ->
                        var result = original
                        
                        // Apply all adjustments in order
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
            } else {
                previewBitmap = null
            }
        }
    }
    
    // Helper function to perform redo
    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val currentState = originalBitmap?.let { bitmap ->
                EditorState(
                    bitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
                    textItems = textItems.map { it.copy() },
                    drawPaths = drawPaths.map { it.copy() },
                    adjustmentValues = adjustmentValues.toMap()
                )
            }
            
            val nextState = redoStack.last()
            redoStack = redoStack.dropLast(1)
            
            currentState?.let { undoStack = undoStack + it }
            
            originalBitmap = nextState.bitmap
            textItems = nextState.textItems
            drawPaths = nextState.drawPaths
            adjustmentValues = nextState.adjustmentValues
            
            // If we're in adjustment mode, re-apply the restored adjustment values to preview
            if (selectedTool?.nameRes == R.string.adjust) {
                coroutineScope.launch(Dispatchers.IO) {
                    originalBitmap?.let { original ->
                        var result = original
                        
                        // Apply all adjustments in order
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
            } else {
                previewBitmap = null
            }
        }
    }
    
    // Helper function to undo saved state (main screen)
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
    
    // Helper function to redo saved state (main screen)
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
    
    // Load original bitmap from URI
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            withContext(Dispatchers.IO) {
                originalBitmap = ImageProcessor.loadBitmap(context, imageUri)
            }
        }
    }
    
    // Показуємо діалог при виборі Text Tool
    LaunchedEffect(selectedTool) {
        if (selectedTool?.nameRes == R.string.text_tool) {
            showTextDialog = true
            dialogInputText = ""
        }
    }
    
    // Handle system back button
    BackHandler(enabled = true) {
        when {
            isEditing -> {
                // Cancel editing
                textItems = emptyList()
                drawPaths = emptyList()
                previewBitmap = null
                adjustmentValues = mapOf(
                    0 to 0f, 1 to 0f, 2 to 0f,
                    3 to 0f, 4 to 0f, 5 to 0f
                )
                showCropOverlay = false
                selectedCropRatio = null
                showTextDialog = false
                selectedTextId = null
                isEditing = false
                selectedTool = null
            }
            selectedTool != null -> {
                // Close tool panel
                selectedTool = null
                showCropOverlay = false
                selectedCropRatio = null
                showTextDialog = false
            }
            else -> {
                // Go back to main screen
                onBackClick()
            }
        }
    }
    
    // Editor tools list
    val editorTools = listOf(
        EditorTool(R.string.crop_rotate, R.drawable.ic_crop),
        EditorTool(R.string.adjust, R.drawable.ic_brightness),
        EditorTool(R.string.ai_tools, R.drawable.ic_ai),
        EditorTool(R.string.ai_effects, R.drawable.ic_filters),
        EditorTool(R.string.text_tool, R.drawable.ic_text),
        EditorTool(R.string.draw_tool, R.drawable.ic_paint)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (!isEditing) {
                        Text(
                            text = stringResource(id = R.string.editing),
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(R.font.font_title)),
                            fontWeight = FontWeight.Bold
                        )
                    } else if (showCropOverlay) {
                        // In crop mode - show only title, no Undo/Redo
                        Text(
                            text = stringResource(id = R.string.crop_rotate),
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(R.font.font_title)),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        // Show Undo/Redo buttons when editing (text/adjustment only)
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { performUndo() },
                                enabled = undoStack.isNotEmpty()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_undo),
                                    contentDescription = "Undo",
                                    tint = if (undoStack.isNotEmpty()) 
                                        MaterialTheme.colorScheme.onBackground 
                                    else 
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                )
                            }
                            
                            IconButton(
                                onClick = { performRedo() },
                                enabled = redoStack.isNotEmpty()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_redo),
                                    contentDescription = "Redo",
                                    tint = if (redoStack.isNotEmpty()) 
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
                            // Cancel editing
                            previewBitmap = null
                            adjustmentValues = mapOf(
                                0 to 0f, 1 to 0f, 2 to 0f,
                                3 to 0f, 4 to 0f, 5 to 0f
                            )
                            showCropOverlay = false
                            selectedCropRatio = null
                            showTextDialog = false
                            selectedTextId = null
                            textItems = emptyList()
                            drawPaths = emptyList()
                            isEditing = false
                            selectedTool = null
                            // Clear undo/redo stacks for editing session
                            undoStack = emptyList()
                            redoStack = emptyList()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = if (isEditing) R.drawable.ic_close else R.drawable.ic_arrow_back),
                            contentDescription = if (isEditing) stringResource(id = R.string.cancel) else stringResource(id = R.string.back)
                        )
                    }
                },
                actions = {
                    // Show Undo/Redo on main screen (not editing)
                    if (!isEditing) {
                        IconButton(
                            onClick = { performSavedStateUndo() },
                            enabled = savedStatesUndoStack.isNotEmpty()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_undo),
                                contentDescription = "Undo",
                                tint = if (savedStatesUndoStack.isNotEmpty()) 
                                    MaterialTheme.colorScheme.onBackground 
                                else 
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                        
                        IconButton(
                            onClick = { performSavedStateRedo() },
                            enabled = savedStatesRedoStack.isNotEmpty()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_redo),
                                contentDescription = "Redo",
                                tint = if (savedStatesRedoStack.isNotEmpty()) 
                                    MaterialTheme.colorScheme.onBackground 
                                else 
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                    }
                    
                    IconButton(onClick = {
                        if (isEditing) {
                            // Save current state before applying changes
                            originalBitmap?.let { bitmap ->
                                savedStatesUndoStack = savedStatesUndoStack + bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                                savedStatesRedoStack = emptyList() // Clear redo when new change is saved
                            }
                            
                            // Apply changes
                            if (showCropOverlay && cropRect != null && imageBounds != null) {
                                // Apply crop with exact coordinates
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
                            
                            // Зберігаємо ВСІ текстові елементи на bitmap 📝
                            if (textItems.isNotEmpty() && selectedTool?.nameRes == R.string.text_tool && imageRectInBox != null && imageBounds != null) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    var resultBitmap = originalBitmap
                                    textItems.forEach { textItem ->
                                        // Враховуємо scale при збереженні
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
                                                textContent = textItem.style.text,
                                                textSize = textSizePx,
                                                textColor = androidColor,
                                                textPosition = drawAbs,
                                                imageBounds = imageBounds!!,
                                                textAlign = android.graphics.Paint.Align.LEFT,
                                                isBold = textItem.style.weight == com.ai.vis.ui.components.TextWeight.BOLD,
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
                            
                            // Зберігаємо ВСІ малюнки на bitmap 🎨
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
                            adjustmentValues = mapOf(
                                0 to 0f, 1 to 0f, 2 to 0f,
                                3 to 0f, 4 to 0f, 5 to 0f
                            )
                            showCropOverlay = false
                            selectedCropRatio = null
                            showTextDialog = false
                            isEditing = false
                            selectedTool = null
                            // Clear editing session undo/redo
                            undoStack = emptyList()
                            redoStack = emptyList()
                        } else {
                            onSaveClick()
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = if (isEditing) R.drawable.ic_done else R.drawable.ic_save),
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
                    .pointerInput(selectedTool, textItems) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                // Одинарний тап для вибору тексту
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
                                            // Оновлюємо textStyle для панелі
                                            textStyle = textItems.find { it.id == selectedTextId }?.style ?: textStyle
                                        }
                                    }
                                } else if (selectedTool != null && selectedTool?.nameRes != R.string.text_tool) {
                                    selectedTool = null
                                }
                            },
                            onDoubleTap = { tapOffset ->
                                // Подвійний тап для редагування тексту
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
                                // Довге натискання для редагування тексту (альтернатива подвійному тапу)
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
                    // Show current image (preview or saved)
                    Image(
                        bitmap = displayBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = density.run { bottomPanelsHeight.toDp() })
                            .onGloballyPositioned { coordinates ->
                                val size = coordinates.size.toSize()
                                val bitmap = displayBitmap ?: return@onGloballyPositioned
                                
                                // Обчислюємо реальний розмір Image з ContentScale.Fit
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
                                
                                // Центруємо image в контейнері
                                val leftInContainer = (size.width - imageWidth) / 2f
                                val topInContainer = (size.height - imageHeight) / 2f
                                
                                // Зберігаємо локальні координати відносно контейнера (Box з padding)
                                // Ці координати використовуються для crop overlay і text positioning
                                imageRectInBox = Rect(
                                    left = leftInContainer,
                                    top = topInContainer,
                                    right = leftInContainer + imageWidth,
                                    bottom = topInContainer + imageHeight
                                )
                                
                                // Для crop та text збереження - використовуємо ті ж локальні координати
                                imageBounds = imageRectInBox
                                
                                android.util.Log.d("PhotoEditor", "🎨 Image bounds (local): $imageBounds")
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
                    // Fallback to original URI while loading
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
                                // Disable zoom/pan in crop mode to keep coordinates consistent
                                if (!showCropOverlay) Modifier.transformable(state = state)
                                else Modifier
                            ),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Hint text when no image
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
                
                // Show crop overlay when in crop mode
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
                        }
                    )
                }
                
                // Show drawing canvas when draw tool is selected
                if (selectedTool?.nameRes == R.string.draw_tool && imageBounds != null) {
                    com.ai.vis.ui.components.DrawingCanvas(
                        imageBounds = imageBounds,
                        drawPaths = drawPaths,
                        currentColor = drawColor,
                        currentStrokeWidth = drawStrokeWidth,
                        currentOpacity = drawOpacity,
                        currentSoftness = drawSoftness,
                        isEraserMode = isEraserMode,
                        onPathAdded = { newPath ->
                            drawPaths = drawPaths + newPath
                        },
                        onDrawingStarted = {
                            // Save state before starting to draw
                            saveStateToUndo()
                            isEditing = true
                        }
                    )
                }
                
                // Відображення ВСіХ текстових елементів з pinch-to-zoom + drag + rotation 📝
                textItems.forEach { textItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(textItem.id) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        // Подвійний тап для редагування
                                        selectedTextId = textItem.id
                                        dialogInputText = textItem.style.text
                                        showTextDialog = true
                                    }
                                )
                            }
                            .pointerInput(textItem.id) {
                                detectTransformGestures(
                                    onGesture = { _, pan, zoom, rotationChange ->
                                        // Save state only at the start of transformation
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
                                // Reset transform flag when touch is released
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
                            color = textItem.style.color.copy(alpha = textItem.style.opacity),
                            fontWeight = when (textItem.style.weight) {
                                com.ai.vis.ui.components.TextWeight.LIGHT -> FontWeight.Light
                                com.ai.vis.ui.components.TextWeight.NORMAL -> FontWeight.Normal
                                com.ai.vis.ui.components.TextWeight.BOLD -> FontWeight.Bold
                            },
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
            }

            // Діалог для введення тексту 💬
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
                        // Редагуємо існуючий текст
                        textItems = textItems.map {
                            if (it.id == selectedTextId) {
                                it.copy(style = it.style.copy(text = text))
                            } else it
                        }
                        // Оновлюємо textStyle щоб він відображав актуальний текст
                        textStyle = textStyle.copy(text = text)
                    } else {
                        // Створюємо новий текст по центру зображення
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

            // Bottom panels with semi-transparent background
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Tool-specific panel (shown when tool is selected with animation)
                AnimatedVisibility(
                    visible = selectedTool != null,
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
                                    onCropRatioSelected = { ratio ->
                                        saveStateToUndo()
                                        selectedCropRatio = ratio
                                        showCropOverlay = true
                                        isEditing = true
                                        // Reset zoom/pan to original position for consistent crop coordinates
                                        scale = 1f
                                        offset = Offset.Zero
                                    },
                                    onRotateLeft = {
                                        // Save current state to main undo stack (like crop)
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
                                        // Save current state to main undo stack (like crop)
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
                                        // Save current state to main undo stack (like crop)
                                        originalBitmap?.let { bitmap ->
                                            savedStatesUndoStack = savedStatesUndoStack + bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                                            savedStatesRedoStack = emptyList()
                                        }
                                        
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { bitmap ->
                                                originalBitmap = ImageProcessor.flipBitmapHorizontal(bitmap)
                                                previewBitmap = null
                                            }
                                        }
                                    },
                                    onFlipVertical = {
                                        // Save current state to main undo stack (like crop)
                                        originalBitmap?.let { bitmap ->
                                            savedStatesUndoStack = savedStatesUndoStack + bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                                            savedStatesRedoStack = emptyList()
                                        }
                                        
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { bitmap ->
                                                originalBitmap = ImageProcessor.flipBitmapVertical(bitmap)
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
                                        // Save state before starting to adjust
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
                                                
                                                // Apply all adjustments in order
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
                                        // Save state after finishing adjustment for proper undo/redo
                                        // This ensures each slider change is a separate undo step
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
                                        // Оновлюємо вибраний текст
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(size = size)) else it
                                            }
                                            isEditing = true
                                        }
                                    },
                                    onColorChange = { color ->
                                        saveStateToUndo()
                                        textStyle = textStyle.copy(color = color)
                                        // Оновлюємо вибраний текст
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
                                        // Циклічна зміна: Normal → Bold → Light → Normal
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
                                        // Тоглова зміна фону
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
                                    onErase = {
                                        // Remove last drawn path (Undo last stroke)
                                        if (drawPaths.isNotEmpty()) {
                                            saveStateToUndo()
                                            drawPaths = drawPaths.dropLast(1)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Bottom tool panel - приховується при редагуванні тексту
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            // Measure only the main menu height, not the tool panel
                            bottomPanelsHeight = coordinates.size.height.toFloat()
                        }
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                ) {
                    when (selectedTool?.nameRes) {
                        R.string.text_tool, R.string.adjust, R.string.draw_tool -> {
                            // Режим редагування з прихованим меню - показуємо кнопку "Назад" та назву
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { 
                                        selectedTool = null
                                        selectedTextId = null
                                        showCropOverlay = false
                                        selectedCropRatio = null
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_back),
                                        contentDescription = stringResource(id = R.string.back),
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
                                
                                // Порожній Box для симетрії
                                Box(modifier = Modifier.size(48.dp))
                            }
                        }
                        else -> {
                            // Звичайне головне меню з інструментами
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
                                            selectedTool = if (selectedTool == tool) null else tool
                                            // Reset text dialog when deselecting text tool
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
            .height(90.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
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

@Preview(showBackground = true)
@Composable
fun PhotoEditorScreenPreview() {
    AIVisTheme {
        PhotoEditorScreen(
            imageUri = null,
            onBackClick = {},
            onSaveClick = {}
        )
    }
}
