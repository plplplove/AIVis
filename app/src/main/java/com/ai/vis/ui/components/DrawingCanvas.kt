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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        val position = change.position
                        // Only draw within image bounds
                        if (imageBounds?.contains(position) == true) {
                            currentPath?.let { path ->
                                path.lineTo(position.x, position.y)
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
                        }
                    }
                )
            }
    ) {
        // Draw all saved paths
        drawPaths.forEach { drawPath ->
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
        
        // Draw current path being drawn
        currentPath?.let { path ->
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
