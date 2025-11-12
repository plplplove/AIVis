package com.ai.vis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

enum class TextAlignment {
    LEFT, CENTER, RIGHT
}

enum class TextWeight {
    LIGHT, NORMAL, BOLD
}

data class TextStyle(
    var text: String = "",
    var size: Float = 24f, // 16-72
    var color: Color = Color.White,
    var alignment: TextAlignment = TextAlignment.CENTER,
    var weight: TextWeight = TextWeight.NORMAL,
    var hasStroke: Boolean = false,
    var hasBackground: Boolean = false
)

data class TextOption(
    val nameRes: Int,
    val iconRes: Int,
    val type: TextOptionType
)

enum class TextOptionType {
    SIZE, COLOR, ALIGNMENT, WEIGHT, STROKE, BACKGROUND
}

@Composable
fun TextPanel(
    textStyle: TextStyle,
    onTextChange: (String) -> Unit = {},
    onSizeChange: (Float) -> Unit = {},
    onColorChange: (Color) -> Unit = {},
    onAlignmentChange: (TextAlignment) -> Unit = {},
    onWeightChange: (TextWeight) -> Unit = {},
    onStrokeToggle: (Boolean) -> Unit = {},
    onBackgroundToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textOptions = listOf(
        TextOption(R.string.text_size, R.drawable.ic_text, TextOptionType.SIZE),
        TextOption(R.string.text_color, R.drawable.ic_palette, TextOptionType.COLOR),
        TextOption(R.string.alignment, R.drawable.ic_align_center, TextOptionType.ALIGNMENT),
        TextOption(R.string.background, R.drawable.ic_background, TextOptionType.BACKGROUND),
        TextOption(R.string.font_weight, R.drawable.ic_text, TextOptionType.WEIGHT)
    )
    
    var selectedOption by remember { mutableStateOf<TextOptionType?>(null) }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Expandable options based on selected option
        AnimatedVisibility(
            visible = selectedOption != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            when (selectedOption) {
                TextOptionType.SIZE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.text_size),
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${textStyle.size.toInt()}",
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Slider(
                            value = textStyle.size,
                            onValueChange = onSizeChange,
                            valueRange = 16f..72f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
                TextOptionType.COLOR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.text_color),
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
                            val colors = listOf(
                                Color.White,
                                Color.Black,
                                Color.Red,
                                Color.Blue,
                                Color.Green,
                                Color.Yellow,
                                Color.Magenta,
                                Color.Cyan
                            )
                            
                            colors.forEach { color ->
                                ColorButton(
                                    color = color,
                                    isSelected = textStyle.color == color,
                                    onClick = { onColorChange(color) }
                                )
                            }
                        }
                    }
                }
                TextOptionType.ALIGNMENT -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AlignmentButton(
                            iconRes = R.drawable.ic_align_left,
                            isSelected = textStyle.alignment == TextAlignment.LEFT,
                            onClick = { onAlignmentChange(TextAlignment.LEFT) },
                            modifier = Modifier.weight(1f)
                        )
                        AlignmentButton(
                            iconRes = R.drawable.ic_align_center,
                            isSelected = textStyle.alignment == TextAlignment.CENTER,
                            onClick = { onAlignmentChange(TextAlignment.CENTER) },
                            modifier = Modifier.weight(1f)
                        )
                        AlignmentButton(
                            iconRes = R.drawable.ic_align_right,
                            isSelected = textStyle.alignment == TextAlignment.RIGHT,
                            onClick = { onAlignmentChange(TextAlignment.RIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                TextOptionType.BACKGROUND -> {
                    // НІЧОГО НЕ ВІДОБРАЖАЄМО - чекбокс працює без розгортання
                }
                TextOptionType.WEIGHT -> {
                    // НІЧОГО НЕ ВІДОБРАЖАЄМО - циклічна кнопка працює без розгортання
                }
                else -> {}
            }
        }
        
        // Horizontal list of text options (like Adjust panel)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(textOptions.size) { index ->
                val option = textOptions[index]
                TextOptionItem(
                    option = option,
                    isSelected = selectedOption == option.type,
                    onClick = {
                        selectedOption = if (selectedOption == option.type) null else option.type
                    }
                )
            }
        }
    }
}

@Composable
fun TextOptionItem(
    option: TextOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Особлива логіка для Weight та Background (циклічні/тогглові кнопки)
    val isToggleType = option.type == TextOptionType.WEIGHT || option.type == TextOptionType.BACKGROUND
    
    Card(
        modifier = modifier
            .width(80.dp)
            .height(80.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && !isToggleType)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected && !isToggleType) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = option.iconRes),
                contentDescription = null,
                tint = if (isSelected && !isToggleType)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = option.nameRes),
                fontSize = 10.sp,
                fontFamily = FontFamily(Font(R.font.font_main_text)),
                color = if (isSelected && !isToggleType)
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

@Composable
fun AlignmentButton(
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun WeightButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.font_main_text)),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                fontWeight = when (text) {
                    "Bold" -> FontWeight.Bold
                    "Light" -> FontWeight.Light
                    else -> FontWeight.Normal
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .background(color, CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun ToggleOption(
    iconRes: Int,
    label: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(56.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.font_main_text)),
                color = if (isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
