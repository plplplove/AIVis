package com.ai.vis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.vis.R

data class DrawParameter(
    val nameRes: Int,
    val iconRes: Int
)

@Composable
fun DrawPanel(
    currentColor: Color,
    currentSize: Float,
    currentOpacity: Float,
    currentSoftness: Float,
    isEraserMode: Boolean,
    currentShapeType: ShapeType,
    isShapeFilled: Boolean,
    onColorChange: (Color) -> Unit,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onSoftnessChange: (Float) -> Unit,
    onEraserToggle: (Boolean) -> Unit,
    onShapeTypeChange: (ShapeType) -> Unit,
    onShapeFillToggle: (Boolean) -> Unit,
    onErase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawParameters = listOf(
        DrawParameter(R.string.brush, R.drawable.ic_paint),
        DrawParameter(R.string.shapes, R.drawable.ic_figure),
        DrawParameter(R.string.draw_color, R.drawable.ic_palette),
        DrawParameter(R.string.draw_size, R.drawable.ic_paint),
        DrawParameter(R.string.draw_opacity, R.drawable.ic_opacity),
        DrawParameter(R.string.softness, R.drawable.ic_blur)
    )
    
    var selectedParameterIndex by remember { mutableIntStateOf(0) } // Start with color selected
    
    val colors = listOf(
        Color.Black,
        Color.White,
        Color.Red,
        Color(0xFFFF6B35), // Orange
        Color(0xFFFFD700), // Gold
        Color.Green,
        Color(0xFF00CED1), // Turquoise
        Color.Blue,
        Color(0xFF9370DB), // Purple
        Color(0xFFFF1493)  // Pink
    )
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Parameter content (shown based on selection)
        AnimatedVisibility(
            visible = true,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(100.dp) // Стандартна висота для всіх секцій
            ) {
                when (selectedParameterIndex) {
                    0 -> {
                        // Brush/Eraser toggle
                        Text(
                            text = if (isEraserMode) stringResource(id = R.string.eraser) else stringResource(id = R.string.brush),
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Brush button
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                onClick = { onEraserToggle(false) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!isEraserMode)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_paint),
                                        contentDescription = null,
                                        tint = if (!isEraserMode)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(id = R.string.brush),
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                                        color = if (!isEraserMode)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            // Eraser button
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                onClick = { onEraserToggle(true) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isEraserMode)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_erase),
                                        contentDescription = null,
                                        tint = if (isEraserMode)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(id = R.string.eraser),
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                                        color = if (isEraserMode)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Shapes selector
                        Text(
                            text = stringResource(id = R.string.shapes),
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val shapes = listOf(
                                ShapeType.LINE to R.drawable.ic_shape_line,
                                ShapeType.ARROW to R.drawable.ic_shape_arrow,
                                ShapeType.RECTANGLE to R.drawable.ic_shape_rectangle,
                                ShapeType.ROUNDED_RECT to R.drawable.ic_shape_rounded_rect,
                                ShapeType.CIRCLE to R.drawable.ic_shape_circle,
                                ShapeType.STAR to R.drawable.ic_shape_star
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(shapes.size) { index ->
                                    val (shape, icon) = shapes[index]
                                    ShapeButton(
                                        shapeType = shape,
                                        iconRes = icon,
                                        isSelected = currentShapeType == shape,
                                        onClick = { onShapeTypeChange(shape) }
                                    )
                                }
                            }
                        }
                        
                        // Fill/Stroke toggle (only for closed shapes, not for lines/arrows)
                        if (currentShapeType != ShapeType.FREE_DRAW && currentShapeType != ShapeType.LINE && currentShapeType != ShapeType.ARROW) {
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = stringResource(id = R.string.draw_style),
                                fontSize = 13.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Fill button
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable { onShapeFillToggle(true) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isShapeFilled)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.shape_fill),
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                                            color = if (isShapeFilled)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                
                                // Stroke button
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable { onShapeFillToggle(false) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!isShapeFilled)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.shape_stroke),
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                                            color = if (!isShapeFilled)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Color picker
                        Text(
                            text = stringResource(id = R.string.draw_color),
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Color grid
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(colors) { color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (currentColor == color) 3.dp else 1.dp,
                                            color = if (currentColor == color)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable { onColorChange(color) }
                                )
                            }
                        }
                    }
                    3 -> {
                        // Brush size slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.draw_size),
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentSize.toInt()}",
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Slider(
                            value = currentSize,
                            onValueChange = onSizeChange,
                            valueRange = 5f..50f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                        )
                    }
                    4 -> {
                        // Opacity slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.draw_opacity),
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(currentOpacity * 100).toInt()}%",
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Slider(
                            value = currentOpacity,
                            onValueChange = onOpacityChange,
                            valueRange = 0.1f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                        )
                    }
                    5 -> {
                        // Softness slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.softness),
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(currentSoftness * 100).toInt()}%",
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Slider(
                            value = currentSoftness,
                            onValueChange = onSoftnessChange,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
        
        // Horizontal list of parameters
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(drawParameters.size) { index ->
                val param = drawParameters[index]
                DrawParameterItem(
                    parameter = param,
                    isSelected = selectedParameterIndex == index,
                    onClick = {
                        selectedParameterIndex = index
                    }
                )
            }
        }
    }
}

@Composable
fun DrawParameterItem(
    parameter: DrawParameter,
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
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
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
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = parameter.iconRes),
                    contentDescription = null,
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(id = parameter.nameRes),
                    fontSize = 10.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 11.sp
                )
            }
        }
    }
}

@Composable
fun ShapeButton(
    shapeType: ShapeType,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shapeName = when (shapeType) {
        ShapeType.FREE_DRAW -> R.string.brush
        ShapeType.LINE -> R.string.shape_line
        ShapeType.ARROW -> R.string.shape_arrow
        ShapeType.RECTANGLE -> R.string.shape_rectangle
        ShapeType.ROUNDED_RECT -> R.string.shape_rounded_rect
        ShapeType.CIRCLE -> R.string.shape_circle
        ShapeType.STAR -> R.string.shape_star
    }
    
    Card(
        modifier = modifier
            .width(60.dp)
            .height(60.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
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
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = shapeName),
                    fontSize = 9.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    lineHeight = 10.sp
                )
            }
        }
    }
}
