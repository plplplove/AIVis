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
    
    // Text tool state - НОВИЙ ПРОСТИЙ ПІДХІД
    var textStyle by remember { mutableStateOf(com.ai.vis.ui.components.TextStyle()) }
    var showTextOverlay by remember { mutableStateOf(false) }
    // Де показати TextField (координати в Box-local space)
    var textFieldPosition by remember { mutableStateOf<Offset?>(null) }
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
                showTextOverlay = false
                textStyle = com.ai.vis.ui.components.TextStyle()
                textFieldPosition = null
                isEditing = false
                selectedTool = null
            }
            selectedTool != null -> {
                // Close tool panel
                selectedTool = null
                showCropOverlay = false
                selectedCropRatio = null
                showTextOverlay = false
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
                            showTextOverlay = false
                            textStyle = com.ai.vis.ui.components.TextStyle()
                            textFieldPosition = null
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
                            
                            // Apply text if in text mode - НОВИЙ ПРОСТИЙ ПІДХІД
                            if (showTextOverlay && selectedTool?.nameRes == R.string.text_tool && 
                                textStyle.text.isNotEmpty() && textFieldPosition != null && imageRectInBox != null && imageBounds != null) {
                                val textSizePxForSave = density.run { textStyle.size.sp.toPx() }
                                coroutineScope.launch(Dispatchers.IO) {
                                    originalBitmap?.let { bitmap ->
                                        val textAlign = when (textStyle.alignment) {
                                            com.ai.vis.ui.components.TextAlignment.LEFT -> android.graphics.Paint.Align.LEFT
                                            com.ai.vis.ui.components.TextAlignment.CENTER -> android.graphics.Paint.Align.CENTER
                                            com.ai.vis.ui.components.TextAlignment.RIGHT -> android.graphics.Paint.Align.RIGHT
                                        }
                                        
                                        val isBold = textStyle.weight == com.ai.vis.ui.components.TextWeight.BOLD
                                        
                                        val androidColor = android.graphics.Color.argb(
                                            (textStyle.color.alpha * 255).toInt(),
                                            (textStyle.color.red * 255).toInt(),
                                            (textStyle.color.green * 255).toInt(),
                                            (textStyle.color.blue * 255).toInt()
                                        )
                                        
                                        // textFieldPosition вже в правильних Box-local координатах
                                        // Конвертуємо до absolute screen coordinates для збереження
                                        val textPosInImage = textFieldPosition!!
                                        val drawAbs = Offset(
                                            imageBounds!!.left + (textPosInImage.x - imageRectInBox!!.left),
                                            imageBounds!!.top + (textPosInImage.y - imageRectInBox!!.top)
                                        )
                                        
                                        originalBitmap = ImageProcessor.drawTextOnBitmap(
                                            bitmap = bitmap,
                                            textContent = textStyle.text,
                                            textSize = textSizePxForSave,
                                            textColor = androidColor,
                                            textPosition = drawAbs,
                                            imageBounds = imageBounds!!,
                                            textAlign = textAlign,
                                            isBold = isBold,
                                            hasStroke = textStyle.hasStroke,
                                            hasBackground = textStyle.hasBackground
                                        )

                                        // Reset text state
                                        textStyle = com.ai.vis.ui.components.TextStyle()
                                        textFieldPosition = null
                                    }
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
                            showTextOverlay = false
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
            // НОВИЙ ПРОСТИЙ КОД - без перетворень координат
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedTool, imageRectInBox, showTextOverlay) {
                        detectTapGestures { tapOffset ->
                            if (selectedTool?.nameRes == R.string.text_tool && imageRectInBox != null) {
                                val imgRect = imageRectInBox!!
                                android.util.Log.d("PhotoEditor", "✅ Тап: $tapOffset, Image rect: $imgRect")
                                
                                // Якщо вже редагуємо текст, зберегти попередній
                                if (showTextOverlay && textStyle.text.isNotEmpty() && textFieldPosition != null && imageBounds != null) {
                                    val textSizePx = density.run { textStyle.size.sp.toPx() }
                                    coroutineScope.launch(Dispatchers.IO) {
                                        originalBitmap?.let { bitmap ->
                                            val textAlign = when (textStyle.alignment) {
                                                com.ai.vis.ui.components.TextAlignment.LEFT -> android.graphics.Paint.Align.LEFT
                                                com.ai.vis.ui.components.TextAlignment.CENTER -> android.graphics.Paint.Align.CENTER
                                                com.ai.vis.ui.components.TextAlignment.RIGHT -> android.graphics.Paint.Align.RIGHT
                                            }
                                            val isBold = textStyle.weight == com.ai.vis.ui.components.TextWeight.BOLD
                                            val androidColor = android.graphics.Color.argb(
                                                (textStyle.color.alpha * 255).toInt(),
                                                (textStyle.color.red * 255).toInt(),
                                                (textStyle.color.green * 255).toInt(),
                                                (textStyle.color.blue * 255).toInt()
                                            )
                                            
                                            // Конвертуємо Box-local координати в absolute для малювання
                                            val drawAbs = Offset(
                                                imageBounds!!.left + (textFieldPosition!!.x - imgRect.left),
                                                imageBounds!!.top + (textFieldPosition!!.y - imgRect.top)
                                            )
                                            
                                            originalBitmap = ImageProcessor.drawTextOnBitmap(
                                                bitmap = bitmap,
                                                textContent = textStyle.text,
                                                textSize = textSizePx,
                                                textColor = androidColor,
                                                textPosition = drawAbs,
                                                imageBounds = imageBounds!!,
                                                textAlign = textAlign,
                                                isBold = isBold,
                                                hasStroke = textStyle.hasStroke,
                                                hasBackground = textStyle.hasBackground
                                            )
                                        }
                                    }
                                    textStyle = com.ai.vis.ui.components.TextStyle()
                                }
                                
                                // Перевірка чи тап всередині зображення
                                if (imgRect.contains(tapOffset)) {
                                    // ПРОСТО зберігаємо tapOffset як є - він вже в Box-local координатах!
                                    textFieldPosition = tapOffset
                                    showTextOverlay = true
                                    isEditing = true
                                    android.util.Log.d("PhotoEditor", "✅ Встановлено textFieldPosition: $tapOffset")
                                } else {
                                    android.util.Log.d("PhotoEditor", "❌ Тап поза зображенням")
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
                
                // НОВИЙ ТЕКСТОВИЙ OVERLAY - АБСОЛЮТНЕ ПОЗИЦІЮВАННЯ ЯК CANVAS 🎯
                if (showTextOverlay && selectedTool?.nameRes == R.string.text_tool && textFieldPosition != null) {
                    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                    
                    LaunchedEffect(showTextOverlay) {
                        if (showTextOverlay) {
                            focusRequester.requestFocus()
                        }
                    }
                    
                    // ВИПРАВЛЕННЯ: використовуємо Layout замість offset для АБСОЛЮТНОГО позиціювання
                    androidx.compose.ui.layout.Layout(
                        content = {
                            Box(
                                modifier = Modifier
                                    .pointerInput(imageRectInBox) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val imgRect = imageRectInBox ?: return@detectDragGestures
                                            val currentPos = textFieldPosition ?: return@detectDragGestures
                                            
                                            // Новa позиція після drag
                                            val newPos = Offset(
                                                x = (currentPos.x + dragAmount.x).coerceIn(imgRect.left, imgRect.right),
                                                y = (currentPos.y + dragAmount.y).coerceIn(imgRect.top, imgRect.bottom)
                                            )
                                            textFieldPosition = newPos
                                            android.util.Log.d("PhotoEditor", "🔄 Drag to: $newPos")
                                        }
                                    }
                            ) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = textStyle.text,
                                    onValueChange = { text ->
                                        textStyle = textStyle.copy(text = text)
                                        isEditing = text.isNotEmpty()
                                        android.util.Log.d("PhotoEditor", "✏️ Text changed: '$text'")
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = textStyle.size.sp,
                                        color = textStyle.color,
                                        fontWeight = when (textStyle.weight) {
                                            com.ai.vis.ui.components.TextWeight.LIGHT -> FontWeight.Light
                                            com.ai.vis.ui.components.TextWeight.NORMAL -> FontWeight.Normal
                                            com.ai.vis.ui.components.TextWeight.BOLD -> FontWeight.Bold
                                        },
                                        textAlign = when (textStyle.alignment) {
                                            com.ai.vis.ui.components.TextAlignment.LEFT -> TextAlign.Left
                                            com.ai.vis.ui.components.TextAlignment.CENTER -> TextAlign.Center
                                            com.ai.vis.ui.components.TextAlignment.RIGHT -> TextAlign.Right
                                        },
                                        shadow = if (textStyle.hasStroke) {
                                            androidx.compose.ui.graphics.Shadow(
                                                color = Color.Black,
                                                offset = Offset(2f, 2f),
                                                blurRadius = 4f
                                            )
                                        } else null
                                    ),
                                    modifier = Modifier
                                        .width(200.dp)
                                        .focusRequester(focusRequester),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    // ЗАВЖДИ показуємо background під час редагування для видимості
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            if (textStyle.text.isEmpty()) {
                                                Text(
                                                    text = stringResource(id = R.string.text_input),
                                                    fontSize = textStyle.size.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { measurables, constraints ->
                        // Вимірюємо TextField
                        val placeable = measurables.firstOrNull()?.measure(constraints)
                        
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable?.let {
                                // Позиціюємо на textFieldPosition (абсолютні координати в Box)
                                val pos = textFieldPosition!!
                                android.util.Log.d("PhotoEditor", "📍 Layout TextField at: $pos")
                                it.placeRelative(pos.x.toInt(), pos.y.toInt())
                            }
                        }
                    }
                }
                
                // 🔴 DEBUG: Червона точка для перевірки координат
                textFieldPosition?.let { pos ->
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.Red,
                            radius = 20f,
                            center = androidx.compose.ui.geometry.Offset(pos.x, pos.y)
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
                                        if (textStyle.text.isNotEmpty()) isEditing = true
                                    },
                                    onColorChange = { color ->
                                        textStyle = textStyle.copy(color = color)
                                        if (textStyle.text.isNotEmpty()) isEditing = true
                                    },
                                    onAlignmentChange = { alignment ->
                                        textStyle = textStyle.copy(alignment = alignment)
                                        if (textStyle.text.isNotEmpty()) isEditing = true
                                    },
                                    onWeightChange = { weight ->
                                        textStyle = textStyle.copy(weight = weight)
                                        if (textStyle.text.isNotEmpty()) isEditing = true
                                    },
                                    onStrokeToggle = { hasStroke ->
                                        textStyle = textStyle.copy(hasStroke = hasStroke)
                                        if (textStyle.text.isNotEmpty()) isEditing = true
                                    },
                                    onBackgroundToggle = { hasBackground ->
                                        textStyle = textStyle.copy(hasBackground = hasBackground)
                                        if (textStyle.text.isNotEmpty()) isEditing = true
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
                                    // Reset text overlay state when deselecting text tool
                                    if (tool.nameRes == R.string.text_tool && selectedTool != tool) {
                                        showTextOverlay = false
                                        textFieldPosition = null
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
