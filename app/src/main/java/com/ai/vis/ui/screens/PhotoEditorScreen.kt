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
    
    // НОВИЙ ПІДХІД: Множинні текстові елементи 📝
    data class TextItem(
        val id: Int,
        var position: Offset,
        var style: com.ai.vis.ui.components.TextStyle
    )
    
    var textItems by remember { mutableStateOf<List<TextItem>>(emptyList()) }
    var selectedTextId by remember { mutableStateOf<Int?>(null) }
    var nextTextId by remember { mutableStateOf(0) }
    var showTextInput by remember { mutableStateOf(false) }
    var currentInputText by remember { mutableStateOf("") }
    var textStyle by remember { mutableStateOf(com.ai.vis.ui.components.TextStyle()) }
    
    // Розмір і позиція Image в Box (для збереження тексту на bitmap)
    var imageRectInBox by remember { mutableStateOf<Rect?>(null) }
    
    // Load original bitmap from URI
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            withContext(Dispatchers.IO) {
                originalBitmap = ImageProcessor.loadBitmap(context, imageUri)
            }
        }
    }
    
    // Handle system back button
    BackHandler(enabled = true) {
        when {
            isEditing -> {
                // Cancel editing
                previewBitmap = null
                adjustmentValues = mapOf(
                    0 to 0f, 1 to 0f, 2 to 0f,
                    3 to 0f, 4 to 0f, 5 to 0f
                )
                showCropOverlay = false
                selectedCropRatio = null
                showTextInput = false
                selectedTextId = null
                textItems = emptyList()
                isEditing = false
                selectedTool = null
            }
            selectedTool != null -> {
                // Close tool panel
                selectedTool = null
                showCropOverlay = false
                selectedCropRatio = null
                showTextInput = false
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
                            showTextInput = false
                            selectedTextId = null
                            textItems = emptyList()
                            isEditing = false
                            selectedTool = null
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
                    IconButton(onClick = {
                        if (isEditing) {
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
                                        if (textItem.style.text.isNotEmpty()) {
                                            val textSizePx = density.run { textItem.style.size.sp.toPx() }
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
                                                    hasBackground = textItem.style.hasBackground
                                                )
                                            }
                                        }
                                    }
                                    originalBitmap = resultBitmap
                                    textItems = emptyList()
                                    showTextInput = false
                                    selectedTextId = null
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
                            showTextInput = false
                            isEditing = false
                            selectedTool = null
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
                    .pointerInput(selectedTool, imageRectInBox) {
                        detectTapGestures { tapOffset ->
                            if (selectedTool?.nameRes == R.string.text_tool && imageRectInBox != null) {
                                val imgRect = imageRectInBox!!
                                
                                // Перевірка чи тапнули на існуючий текст
                                val tappedText = textItems.find { item ->
                                    val bounds = Rect(
                                        item.position.x - 50f,
                                        item.position.y - 50f,
                                        item.position.x + 200f,
                                        item.position.y + 50f
                                    )
                                    bounds.contains(tapOffset)
                                }
                                
                                if (tappedText != null) {
                                    // Редагуємо існуючий текст
                                    selectedTextId = tappedText.id
                                    currentInputText = tappedText.style.text
                                    showTextInput = true
                                    isEditing = true
                                } else if (imgRect.contains(tapOffset)) {
                                    // Створюємо новий текст
                                    val newId = nextTextId
                                    nextTextId++
                                    val newItem = TextItem(
                                        id = newId,
                                        position = tapOffset,
                                        style = com.ai.vis.ui.components.TextStyle(text = "")
                                    )
                                    textItems = textItems + newItem
                                    selectedTextId = newId
                                    currentInputText = ""
                                    showTextInput = true
                                    isEditing = true
                                }
                            } else if (selectedTool != null) {
                                selectedTool = null
                            }
                        }
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
                                // НОВИЙ КОД - простіше обчислення
                                val posInWindow = coordinates.positionInWindow()
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
                                
                                // Центруємо image в Box
                                val leftInBox = (size.width - imageWidth) / 2f
                                val topInBox = (size.height - imageHeight) / 2f
                                
                                // Зберігаємо Box-local rect (відносно Box з detectTapGestures)
                                imageRectInBox = Rect(
                                    left = leftInBox,
                                    top = topInBox,
                                    right = leftInBox + imageWidth,
                                    bottom = topInBox + imageHeight
                                )
                                
                                // Зберігаємо absolute rect для crop та збереження
                                imageBounds = Rect(
                                    left = posInWindow.x + leftInBox,
                                    top = posInWindow.y + topInBox,
                                    right = posInWindow.x + leftInBox + imageWidth,
                                    bottom = posInWindow.y + topInBox + imageHeight
                                )
                                
                                android.util.Log.d("PhotoEditor", "🎨 Image rect (Box-local): $imageRectInBox")
                                android.util.Log.d("PhotoEditor", "🌍 Image bounds (absolute): $imageBounds")
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
                
                // Відображення ВСіХ текстових елементів 📝
                textItems.forEach { textItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(textItem.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val imgRect = imageRectInBox ?: return@detectDragGestures
                                    val newPos = Offset(
                                        x = (textItem.position.x + dragAmount.x).coerceIn(imgRect.left, imgRect.right),
                                        y = (textItem.position.y + dragAmount.y).coerceIn(imgRect.top, imgRect.bottom)
                                    )
                                    textItems = textItems.map {
                                        if (it.id == textItem.id) it.copy(position = newPos) else it
                                    }
                                }
                            }
                    ) {
                        Text(
                            text = textItem.style.text.ifEmpty { "Tap to type" },
                            fontSize = textItem.style.size.sp,
                            color = textItem.style.color,
                            fontWeight = when (textItem.style.weight) {
                                com.ai.vis.ui.components.TextWeight.LIGHT -> FontWeight.Light
                                com.ai.vis.ui.components.TextWeight.NORMAL -> FontWeight.Normal
                                com.ai.vis.ui.components.TextWeight.BOLD -> FontWeight.Bold
                            },
                            modifier = Modifier
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        textItem.position.x.toInt(),
                                        textItem.position.y.toInt()
                                    )
                                }
                                .background(
                                    color = if (textItem.style.hasBackground)
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
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
                
                // TextField для введення внизу екрану
                if (showTextInput && selectedTextId != null) {
                    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                    val selectedItem = textItems.find { it.id == selectedTextId }
                    
                    LaunchedEffect(showTextInput) {
                        if (showTextInput) {
                            focusRequester.requestFocus()
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = currentInputText,
                            onValueChange = { text ->
                                currentInputText = text
                                textItems = textItems.map {
                                    if (it.id == selectedTextId) {
                                        it.copy(style = it.style.copy(text = text))
                                    } else it
                                }
                                isEditing = text.isNotEmpty()
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = selectedItem?.style?.size?.sp ?: 24.sp,
                                color = selectedItem?.style?.color ?: Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.padding(8.dp)) {
                                    if (currentInputText.isEmpty()) {
                                        Text(
                                            text = stringResource(id = R.string.text_input),
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

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
                                        selectedCropRatio = ratio
                                        showCropOverlay = true
                                        isEditing = true
                                        // Reset zoom/pan to original position for consistent crop coordinates
                                        scale = 1f
                                        offset = Offset.Zero
                                    },
                                    onRotateLeft = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { bitmap ->
                                                originalBitmap = ImageProcessor.rotateBitmap(bitmap, -90f)
                                                previewBitmap = null
                                            }
                                        }
                                    },
                                    onRotateRight = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { bitmap ->
                                                originalBitmap = ImageProcessor.rotateBitmap(bitmap, 90f)
                                                previewBitmap = null
                                            }
                                        }
                                    },
                                    onFlipHorizontal = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            originalBitmap?.let { bitmap ->
                                                originalBitmap = ImageProcessor.flipBitmapHorizontal(bitmap)
                                                previewBitmap = null
                                            }
                                        }
                                    },
                                    onFlipVertical = {
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
                                        textStyle = textStyle.copy(size = size)
                                        // Оновлюємо всі текстові елементи з новим розміром
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(size = size)) else it
                                            }
                                        }
                                        if (currentInputText.isNotEmpty()) isEditing = true
                                    },
                                    onColorChange = { color ->
                                        textStyle = textStyle.copy(color = color)
                                        // Оновлюємо всі текстові елементи з новим кольором
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(color = color)) else it
                                            }
                                        }
                                        if (currentInputText.isNotEmpty()) isEditing = true
                                    },
                                    onAlignmentChange = { alignment ->
                                        textStyle = textStyle.copy(alignment = alignment)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(alignment = alignment)) else it
                                            }
                                        }
                                        if (currentInputText.isNotEmpty()) isEditing = true
                                    },
                                    onWeightChange = { weight ->
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
                                        }
                                        if (currentInputText.isNotEmpty()) isEditing = true
                                    },
                                    onStrokeToggle = { hasStroke ->
                                        textStyle = textStyle.copy(hasStroke = hasStroke)
                                        if (currentInputText.isNotEmpty()) isEditing = true
                                    },
                                    onBackgroundToggle = { hasBackground ->
                                        // Тоглова зміна фону
                                        val newBackground = !textStyle.hasBackground
                                        textStyle = textStyle.copy(hasBackground = newBackground)
                                        if (selectedTextId != null) {
                                            textItems = textItems.map {
                                                if (it.id == selectedTextId) it.copy(style = it.style.copy(hasBackground = newBackground)) else it
                                            }
                                        }
                                        if (currentInputText.isNotEmpty()) isEditing = true
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Bottom tool panel (always visible)
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
                                    // Reset text input when deselecting text tool
                                    if (tool.nameRes == R.string.text_tool && selectedTool != tool) {
                                        showTextInput = false
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
