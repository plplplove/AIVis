package com.ai.vis.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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

// Shape types for drawing
enum class ShapeType {
    FREE_DRAW,  // Normal drawing
    LINE,
    ARROW,
    RECTANGLE,
    ROUNDED_RECT,
    CIRCLE,
    STAR
}

// Data class to store a drawing path with its properties
data class DrawPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val opacity: Float,
    val softness: Float = 0f, // 0 = sharp, higher = blurry edges
    val isEraser: Boolean = false, // true = eraser mode
    val shapeType: ShapeType = ShapeType.FREE_DRAW,
    val isFilled: Boolean = false, // true = filled shape, false = stroke only
    val startPoint: Offset? = null, // For shapes
    val endPoint: Offset? = null // For shapes
)

// Helper function to create shape paths
fun createShapePath(shapeType: ShapeType, start: Offset, end: Offset): Path {
    val path = Path()
    
    when (shapeType) {
        ShapeType.LINE -> {
            path.moveTo(start.x, start.y)
            path.lineTo(end.x, end.y)
        }
        ShapeType.ARROW -> {
            // Draw line
            path.moveTo(start.x, start.y)
            path.lineTo(end.x, end.y)
            
            // Draw arrowhead
            val angle = kotlin.math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
            val arrowLength = 30f
            val arrowAngle = Math.PI / 6 // 30 degrees
            
            val arrow1X = end.x - arrowLength * kotlin.math.cos(angle - arrowAngle).toFloat()
            val arrow1Y = end.y - arrowLength * kotlin.math.sin(angle - arrowAngle).toFloat()
            path.moveTo(end.x, end.y)
            path.lineTo(arrow1X, arrow1Y)
            
            val arrow2X = end.x - arrowLength * kotlin.math.cos(angle + arrowAngle).toFloat()
            val arrow2Y = end.y - arrowLength * kotlin.math.sin(angle + arrowAngle).toFloat()
            path.moveTo(end.x, end.y)
            path.lineTo(arrow2X, arrow2Y)
        }
        ShapeType.RECTANGLE -> {
            path.addRect(
                androidx.compose.ui.geometry.Rect(
                    left = minOf(start.x, end.x),
                    top = minOf(start.y, end.y),
                    right = maxOf(start.x, end.x),
                    bottom = maxOf(start.y, end.y)
                )
            )
        }
        ShapeType.ROUNDED_RECT -> {
            val rect = androidx.compose.ui.geometry.Rect(
                left = minOf(start.x, end.x),
                top = minOf(start.y, end.y),
                right = maxOf(start.x, end.x),
                bottom = maxOf(start.y, end.y)
            )
            val radius = minOf(rect.width, rect.height) * 0.2f
            path.addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = rect,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
            )
        }
        ShapeType.CIRCLE -> {
            val center = Offset(
                (start.x + end.x) / 2f,
                (start.y + end.y) / 2f
            )
            val radius = kotlin.math.sqrt(
                ((end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)) / 4f
            )
            path.addOval(
                androidx.compose.ui.geometry.Rect(
                    center = center,
                    radius = radius
                )
            )
        }
        ShapeType.STAR -> {
            // Draw 5-pointed star
            val centerX = (start.x + end.x) / 2f
            val centerY = (start.y + end.y) / 2f
            val radius = kotlin.math.sqrt(
                ((end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)) / 4f
            )
            val innerRadius = radius * 0.4f
            val points = 5
            
            for (i in 0 until points * 2) {
                val angle = Math.PI / 2 + (i * Math.PI / points)
                val r = if (i % 2 == 0) radius else innerRadius
                val x = centerX + r * kotlin.math.cos(angle).toFloat()
                val y = centerY - r * kotlin.math.sin(angle).toFloat()
                
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()
        }
        ShapeType.FREE_DRAW -> {
            // Should not happen, but handle gracefully
        }
    }
    
    return path
}

@Composable
fun DrawingCanvas(
    imageBounds: Rect?,
    drawPaths: List<DrawPath>,
    currentColor: Color,
    currentStrokeWidth: Float,
    currentOpacity: Float,
    currentSoftness: Float,
    isEraserMode: Boolean,
    currentShapeType: ShapeType = ShapeType.FREE_DRAW,
    isShapeFilled: Boolean = false,
    onPathAdded: (DrawPath) -> Unit,
    onDrawingStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var shapeStartPoint by remember { mutableStateOf<Offset?>(null) }
    var shapeEndPoint by remember { mutableStateOf<Offset?>(null) }
    var pathUpdateTrigger by remember { mutableStateOf(0) }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(currentColor, currentStrokeWidth, currentOpacity, currentSoftness, isEraserMode, currentShapeType, isShapeFilled) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Only allow drawing within image bounds
                        if (imageBounds?.contains(offset) == true) {
                            onDrawingStarted()
                            
                            if (currentShapeType == ShapeType.FREE_DRAW) {
                                // Normal free drawing
                                currentPath = Path().apply {
                                    moveTo(offset.x, offset.y)
                                    lineTo(offset.x + 0.1f, offset.y + 0.1f)
                                }
                            } else {
                                // Shape drawing - remember start point
                                shapeStartPoint = offset
                                shapeEndPoint = offset
                                currentPath = Path() // Empty path for now
                            }
                            pathUpdateTrigger++
                        }
                    },
                    onDrag = { change, _ ->
                        val position = change.position
                        if (imageBounds?.contains(position) == true) {
                            if (currentShapeType == ShapeType.FREE_DRAW) {
                                // Normal free drawing
                                currentPath?.let { path ->
                                    path.lineTo(position.x, position.y)
                                    pathUpdateTrigger++
                                }
                            } else {
                                // Update shape end point for preview
                                shapeEndPoint = position
                                pathUpdateTrigger++
                            }
                        }
                    },
                    onDragEnd = {
                        if (currentShapeType == ShapeType.FREE_DRAW) {
                            // Save normal path
                            currentPath?.let { path ->
                                onPathAdded(
                                    DrawPath(
                                        path = Path().apply { addPath(path) },
                                        color = currentColor,
                                        strokeWidth = currentStrokeWidth,
                                        opacity = currentOpacity,
                                        softness = currentSoftness,
                                        isEraser = isEraserMode
                                    )
                                )
                            }
                        } else {
                            // Save shape
                            if (shapeStartPoint != null && shapeEndPoint != null) {
                                val shapePath = createShapePath(
                                    shapeType = currentShapeType,
                                    start = shapeStartPoint!!,
                                    end = shapeEndPoint!!
                                )
                                onPathAdded(
                                    DrawPath(
                                        path = shapePath,
                                        color = currentColor,
                                        strokeWidth = currentStrokeWidth,
                                        opacity = currentOpacity,
                                        softness = currentSoftness,
                                        isEraser = isEraserMode,
                                        shapeType = currentShapeType,
                                        isFilled = isShapeFilled,
                                        startPoint = shapeStartPoint,
                                        endPoint = shapeEndPoint
                                    )
                                )
                            }
                        }
                        currentPath = null
                        shapeStartPoint = null
                        shapeEndPoint = null
                        pathUpdateTrigger++
                    }
                )
            }
    ) {
        // Use pathUpdateTrigger to force recomposition when path changes
        pathUpdateTrigger.let { _ ->
            // Малюємо всі шляхи на окремому layer, щоб eraser стирав тільки малюнок
            drawIntoCanvas { canvas ->
                // Створюємо окремий layer для малювання
                val layerPaint = android.graphics.Paint()
                val layerBounds = android.graphics.RectF(0f, 0f, size.width, size.height)
                canvas.nativeCanvas.saveLayer(layerBounds, layerPaint)
                
                // Draw all saved paths on the layer
                drawPaths.forEach { drawPath ->
                    val paint = Paint().apply {
                        strokeWidth = drawPath.strokeWidth
                        strokeCap = StrokeCap.Round
                        strokeJoin = StrokeJoin.Round
                        style = if (drawPath.isFilled && drawPath.shapeType != ShapeType.FREE_DRAW) {
                            androidx.compose.ui.graphics.PaintingStyle.Fill
                        } else {
                            androidx.compose.ui.graphics.PaintingStyle.Stroke
                        }
                        isAntiAlias = true
                        
                        if (drawPath.isEraser) {
                            // Eraser - використовуємо DST_OUT для стирання тільки малюнка
                            asFrameworkPaint().xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                            // Для eraser використовуємо повну непрозорість щоб добре стирало
                            color = Color.Black
                        } else {
                            // Normal drawing
                            color = drawPath.color.copy(alpha = drawPath.opacity)
                        }
                        
                        // Apply blur/softness if needed
                        if (drawPath.softness > 0f) {
                            val blurRadius = drawPath.softness * (drawPath.strokeWidth / 2f)
                            if (blurRadius > 0f) {
                                asFrameworkPaint().maskFilter = BlurMaskFilter(
                                    blurRadius,
                                    BlurMaskFilter.Blur.NORMAL
                                )
                            }
                        }
                    }
                    
                    canvas.nativeCanvas.drawPath(
                        drawPath.path.asAndroidPath(),
                        paint.asFrameworkPaint()
                    )
                }
                
                // Draw shape preview while dragging
                if (currentShapeType != ShapeType.FREE_DRAW && shapeStartPoint != null && shapeEndPoint != null) {
                    val previewPath = createShapePath(currentShapeType, shapeStartPoint!!, shapeEndPoint!!)
                    val previewPaint = Paint().apply {
                        strokeWidth = currentStrokeWidth
                        strokeCap = StrokeCap.Round
                        strokeJoin = StrokeJoin.Round
                        style = if (isShapeFilled) {
                            androidx.compose.ui.graphics.PaintingStyle.Fill
                        } else {
                            androidx.compose.ui.graphics.PaintingStyle.Stroke
                        }
                        isAntiAlias = true
                        color = currentColor.copy(alpha = currentOpacity * 0.7f) // Semi-transparent preview
                        
                        if (currentSoftness > 0f) {
                            val blurRadius = currentSoftness * (currentStrokeWidth / 2f)
                            if (blurRadius > 0f) {
                                asFrameworkPaint().maskFilter = BlurMaskFilter(
                                    blurRadius,
                                    BlurMaskFilter.Blur.NORMAL
                                )
                            }
                        }
                    }
                    
                    canvas.nativeCanvas.drawPath(
                        previewPath.asAndroidPath(),
                        previewPaint.asFrameworkPaint()
                    )
                }
                
                // Draw current free-draw path being drawn
                if (currentShapeType == ShapeType.FREE_DRAW) {
                    currentPath?.let { path ->
                        val paint = Paint().apply {
                            strokeWidth = currentStrokeWidth
                            strokeCap = StrokeCap.Round
                            strokeJoin = StrokeJoin.Round
                            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                            isAntiAlias = true
                            
                            if (isEraserMode) {
                                // Eraser - використовуємо DST_OUT для стирання
                                asFrameworkPaint().xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                                color = Color.Black
                            } else {
                                // Normal drawing
                                color = currentColor.copy(alpha = currentOpacity)
                            }
                            
                            // Apply blur/softness if needed
                            if (currentSoftness > 0f) {
                                val blurRadius = currentSoftness * (currentStrokeWidth / 2f)
                                if (blurRadius > 0f) {
                                    asFrameworkPaint().maskFilter = BlurMaskFilter(
                                        blurRadius,
                                        BlurMaskFilter.Blur.NORMAL
                                    )
                                }
                            }
                        }
                        
                        canvas.nativeCanvas.drawPath(
                            path.asAndroidPath(),
                            paint.asFrameworkPaint()
                        )
                    }
                }
                
                // Restore canvas
                canvas.nativeCanvas.restore()
            }
        }
    }
}