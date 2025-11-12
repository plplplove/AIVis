package com.ai.vis.ui.components

import android.graphics.BlurMaskFilter
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.input.pointer.pointerInput

// Data class to store a drawing path with its properties
data class DrawPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val opacity: Float,
    val softness: Float = 0f // 0 = sharp, higher = blurry edges
)

@Composable
fun DrawingCanvas(
    imageBounds: Rect?,
    drawPaths: List<DrawPath>,
    currentColor: Color,
    currentStrokeWidth: Float,
    currentOpacity: Float,
    currentSoftness: Float,
    onPathAdded: (DrawPath) -> Unit,
    onDrawingStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var pathUpdateTrigger by remember { mutableStateOf(0) }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(currentColor, currentStrokeWidth, currentOpacity, currentSoftness) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Only allow drawing within image bounds
                        if (imageBounds?.contains(offset) == true) {
                            onDrawingStarted()
                            currentPath = Path().apply {
                                moveTo(offset.x, offset.y)
                                // Add a tiny line to make the initial point visible
                                lineTo(offset.x + 0.1f, offset.y + 0.1f)
                            }
                            pathUpdateTrigger++
                        }
                    },
                    onDrag = { change, _ ->
                        val position = change.position
                        // Only draw within image bounds
                        if (imageBounds?.contains(position) == true) {
                            currentPath?.let { path ->
                                // Draw in real-time - add point immediately
                                path.lineTo(position.x, position.y)
                                // Trigger recomposition to show the line in real-time
                                pathUpdateTrigger++
                            }
                        }
                    },
                    onDragEnd = {
                        currentPath?.let { path ->
                            // Save the completed path
                            onPathAdded(
                                DrawPath(
                                    path = Path().apply { addPath(path) }, // Create a copy
                                    color = currentColor,
                                    strokeWidth = currentStrokeWidth,
                                    opacity = currentOpacity,
                                    softness = currentSoftness
                                )
                            )
                            currentPath = null
                            pathUpdateTrigger++
                        }
                    }
                )
            }
    ) {
        // Use pathUpdateTrigger to force recomposition when path changes
        pathUpdateTrigger.let { _ ->
            // Draw all saved paths
            drawPaths.forEach { drawPath ->
                // If softness is applied, use native canvas for blur
                if (drawPath.softness > 0f) {
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = drawPath.color.copy(alpha = drawPath.opacity)
                            strokeWidth = drawPath.strokeWidth
                            strokeCap = StrokeCap.Round
                            strokeJoin = StrokeJoin.Round
                            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                            isAntiAlias = true
                        }
                        
                        // Apply blur mask filter for softness
                        val blurRadius = drawPath.softness * (drawPath.strokeWidth / 2f)
                        if (blurRadius > 0f) {
                            paint.asFrameworkPaint().maskFilter = BlurMaskFilter(
                                blurRadius,
                                BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        
                        canvas.nativeCanvas.drawPath(
                            drawPath.path.asAndroidPath(),
                            paint.asFrameworkPaint()
                        )
                    }
                } else {
                    // No softness - use regular drawing for better performance
                    drawPath(
                        path = drawPath.path,
                        color = drawPath.color.copy(alpha = drawPath.opacity),
                        style = Stroke(
                            width = drawPath.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
            
            // Draw current path being drawn
            currentPath?.let { path ->
                // If softness is applied, use native canvas for blur
                if (currentSoftness > 0f) {
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = currentColor.copy(alpha = currentOpacity)
                            strokeWidth = currentStrokeWidth
                            strokeCap = StrokeCap.Round
                            strokeJoin = StrokeJoin.Round
                            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                            isAntiAlias = true
                        }
                        
                        // Apply blur mask filter for softness
                        val blurRadius = currentSoftness * (currentStrokeWidth / 2f)
                        if (blurRadius > 0f) {
                            paint.asFrameworkPaint().maskFilter = BlurMaskFilter(
                                blurRadius,
                                BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        
                        canvas.nativeCanvas.drawPath(
                            path.asAndroidPath(),
                            paint.asFrameworkPaint()
                        )
                    }
                } else {
                    // No softness - use regular drawing for better performance
                    drawPath(
                        path = path,
                        color = currentColor.copy(alpha = currentOpacity),
                        style = Stroke(
                            width = currentStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
