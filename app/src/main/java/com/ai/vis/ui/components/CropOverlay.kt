package com.ai.vis.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class DragHandle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}

@Composable
fun CropOverlay(
    cropRatio: Float?, // null for free, 1f for 1:1, etc.
    imageBounds: Rect?, // Bounds of the actual image on screen
    scale: Float = 1f, // Current zoom scale
    offset: Offset = Offset.Zero, // Current pan offset
    onCropAreaChange: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val handleSize = with(density) { 44.dp.toPx() } // Touch target size
    val handleVisualSize = with(density) { 24.dp.toPx() } // Visual size
    
    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    
    // Calculate transformed bounds
    val transformedBounds = remember(imageBounds, scale, offset, canvasSize) {
        if (imageBounds != null && canvasSize != Size.Zero) {
            val centerX = canvasSize.width / 2f
            val centerY = canvasSize.height / 2f
            
            // Calculate scaled dimensions
            val scaledWidth = imageBounds.width * scale
            val scaledHeight = imageBounds.height * scale
            
            // Calculate new position with scale and offset
            val scaledLeft = centerX - (centerX - imageBounds.left) * scale + offset.x
            val scaledTop = centerY - (centerY - imageBounds.top) * scale + offset.y
            
            Rect(
                left = scaledLeft,
                top = scaledTop,
                right = scaledLeft + scaledWidth,
                bottom = scaledTop + scaledHeight
            )
        } else {
            null
        }
    }
    
    // Initialize crop rect based on ratio
    var cropRect by remember(cropRatio, transformedBounds) {
        mutableStateOf<Rect?>(null)
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(cropRatio, scale, offset) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val rect = cropRect ?: return@detectDragGestures
                        
                        // Determine which handle was touched
                        activeHandle = when {
                            isNearPoint(offset, Offset(rect.left, rect.top), handleSize) -> DragHandle.TOP_LEFT
                            isNearPoint(offset, Offset(rect.right, rect.top), handleSize) -> DragHandle.TOP_RIGHT
                            isNearPoint(offset, Offset(rect.left, rect.bottom), handleSize) -> DragHandle.BOTTOM_LEFT
                            isNearPoint(offset, Offset(rect.right, rect.bottom), handleSize) -> DragHandle.BOTTOM_RIGHT
                            rect.contains(offset) -> DragHandle.CENTER
                            else -> DragHandle.NONE
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val rect = cropRect ?: return@detectDragGestures
                        
                        cropRect = when (activeHandle) {
                            DragHandle.TOP_LEFT -> {
                                if (cropRatio != null) {
                                    // Maintain aspect ratio
                                    val newWidth = (rect.right - (rect.left + dragAmount.x)).coerceAtLeast(100f)
                                    val newHeight = newWidth / cropRatio
                                    Rect(
                                        left = rect.right - newWidth,
                                        top = rect.bottom - newHeight,
                                        right = rect.right,
                                        bottom = rect.bottom
                                    )
                                } else {
                                    rect.copy(
                                        left = (rect.left + dragAmount.x).coerceAtMost(rect.right - 100f),
                                        top = (rect.top + dragAmount.y).coerceAtMost(rect.bottom - 100f)
                                    )
                                }
                            }
                            DragHandle.TOP_RIGHT -> {
                                if (cropRatio != null) {
                                    val newWidth = (rect.left + dragAmount.x - rect.left + (rect.right - rect.left)).coerceAtLeast(100f)
                                    val newHeight = newWidth / cropRatio
                                    Rect(
                                        left = rect.left,
                                        top = rect.bottom - newHeight,
                                        right = rect.left + newWidth,
                                        bottom = rect.bottom
                                    )
                                } else {
                                    rect.copy(
                                        right = (rect.right + dragAmount.x).coerceAtLeast(rect.left + 100f),
                                        top = (rect.top + dragAmount.y).coerceAtMost(rect.bottom - 100f)
                                    )
                                }
                            }
                            DragHandle.BOTTOM_LEFT -> {
                                if (cropRatio != null) {
                                    val newWidth = (rect.right - (rect.left + dragAmount.x)).coerceAtLeast(100f)
                                    val newHeight = newWidth / cropRatio
                                    Rect(
                                        left = rect.right - newWidth,
                                        top = rect.top,
                                        right = rect.right,
                                        bottom = rect.top + newHeight
                                    )
                                } else {
                                    rect.copy(
                                        left = (rect.left + dragAmount.x).coerceAtMost(rect.right - 100f),
                                        bottom = (rect.bottom + dragAmount.y).coerceAtLeast(rect.top + 100f)
                                    )
                                }
                            }
                            DragHandle.BOTTOM_RIGHT -> {
                                if (cropRatio != null) {
                                    val newWidth = (rect.right + dragAmount.x - rect.left).coerceAtLeast(100f)
                                    val newHeight = newWidth / cropRatio
                                    Rect(
                                        left = rect.left,
                                        top = rect.top,
                                        right = rect.left + newWidth,
                                        bottom = rect.top + newHeight
                                    )
                                } else {
                                    rect.copy(
                                        right = (rect.right + dragAmount.x).coerceAtLeast(rect.left + 100f),
                                        bottom = (rect.bottom + dragAmount.y).coerceAtLeast(rect.top + 100f)
                                    )
                                }
                            }
                            DragHandle.CENTER -> {
                                // Move entire rect within bounds
                                val dragBounds = transformedBounds ?: Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
                                val newLeft = (rect.left + dragAmount.x).coerceIn(dragBounds.left, dragBounds.right - rect.width)
                                val newTop = (rect.top + dragAmount.y).coerceIn(dragBounds.top, dragBounds.bottom - rect.height)
                                Rect(
                                    left = newLeft,
                                    top = newTop,
                                    right = newLeft + rect.width,
                                    bottom = newTop + rect.height
                                )
                            }
                            DragHandle.NONE -> rect
                        }
                        
                        // Constrain crop rect to image bounds
                        cropRect = cropRect?.let { r ->
                            val constrainBounds = transformedBounds ?: Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
                            Rect(
                                left = r.left.coerceIn(constrainBounds.left, constrainBounds.right),
                                top = r.top.coerceIn(constrainBounds.top, constrainBounds.bottom),
                                right = r.right.coerceIn(constrainBounds.left, constrainBounds.right),
                                bottom = r.bottom.coerceIn(constrainBounds.top, constrainBounds.bottom)
                            )
                        }
                    },
                    onDragEnd = {
                        cropRect?.let { onCropAreaChange(it) }
                        activeHandle = DragHandle.NONE
                    }
                )
            }
    ) {
        // Update canvas size
        canvasSize = size
        
        // Use transformed bounds if available, otherwise use full canvas
        val bounds = transformedBounds ?: Rect(0f, 0f, size.width, size.height)
        
        // Initialize crop rect if not set
        if (cropRect == null) {
            val centerX = bounds.center.x
            val centerY = bounds.center.y
            
            // Use image bounds as maximum
            val maxWidth = bounds.width
            val maxHeight = bounds.height
            
            val (width, height) = if (cropRatio != null) {
                // Calculate dimensions maintaining aspect ratio
                val ratioWidth = maxWidth
                val ratioHeight = ratioWidth / cropRatio
                
                if (ratioHeight <= maxHeight) {
                    ratioWidth to ratioHeight
                } else {
                    val newHeight = maxHeight
                    val newWidth = newHeight * cropRatio
                    newWidth to newHeight
                }
            } else {
                // Free crop - use full image
                maxWidth to maxHeight
            }
            
            cropRect = Rect(
                left = centerX - width / 2,
                top = centerY - height / 2,
                right = centerX + width / 2,
                bottom = centerY + height / 2
            )
        }
        
        val rect = cropRect ?: return@Canvas
        
        // Draw semi-transparent overlay (darkened areas)
        val overlayPath = Path().apply {
            // Outer rectangle (full canvas)
            addRect(Rect(0f, 0f, size.width, size.height))
            // Inner rectangle (crop area) - subtract it
            addRect(rect)
        }
        
        drawPath(
            path = overlayPath,
            color = Color.Black.copy(alpha = 0.5f),
            style = Fill
        )
        
        // Draw crop border
        drawRect(
            color = Color.White,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Draw grid lines (rule of thirds)
        val gridColor = Color.White.copy(alpha = 0.5f)
        val gridStroke = Stroke(width = 1.dp.toPx())
        
        // Vertical lines
        drawLine(
            color = gridColor,
            start = Offset(rect.left + rect.width / 3, rect.top),
            end = Offset(rect.left + rect.width / 3, rect.bottom),
            strokeWidth = gridStroke.width
        )
        drawLine(
            color = gridColor,
            start = Offset(rect.left + 2 * rect.width / 3, rect.top),
            end = Offset(rect.left + 2 * rect.width / 3, rect.bottom),
            strokeWidth = gridStroke.width
        )
        
        // Horizontal lines
        drawLine(
            color = gridColor,
            start = Offset(rect.left, rect.top + rect.height / 3),
            end = Offset(rect.right, rect.top + rect.height / 3),
            strokeWidth = gridStroke.width
        )
        drawLine(
            color = gridColor,
            start = Offset(rect.left, rect.top + 2 * rect.height / 3),
            end = Offset(rect.right, rect.top + 2 * rect.height / 3),
            strokeWidth = gridStroke.width
        )
        
        // Draw corner handles
        val handleColor = Color.White
        val cornerHandles = listOf(
            Offset(rect.left, rect.top),
            Offset(rect.right, rect.top),
            Offset(rect.left, rect.bottom),
            Offset(rect.right, rect.bottom)
        )
        
        cornerHandles.forEach { corner ->
            // Draw L-shaped corner handle
            val lineLength = handleVisualSize
            val thickness = 4.dp.toPx()
            
            val isLeft = corner.x == rect.left
            val isTop = corner.y == rect.top
            
            // Horizontal line
            drawLine(
                color = handleColor,
                start = Offset(
                    if (isLeft) corner.x else corner.x - lineLength,
                    corner.y
                ),
                end = Offset(
                    if (isLeft) corner.x + lineLength else corner.x,
                    corner.y
                ),
                strokeWidth = thickness
            )
            
            // Vertical line
            drawLine(
                color = handleColor,
                start = Offset(
                    corner.x,
                    if (isTop) corner.y else corner.y - lineLength
                ),
                end = Offset(
                    corner.x,
                    if (isTop) corner.y + lineLength else corner.y
                ),
                strokeWidth = thickness
            )
        }
    }
}

private fun isNearPoint(touch: Offset, target: Offset, threshold: Float): Boolean {
    val dx = abs(touch.x - target.x)
    val dy = abs(touch.y - target.y)
    return dx < threshold && dy < threshold
}
