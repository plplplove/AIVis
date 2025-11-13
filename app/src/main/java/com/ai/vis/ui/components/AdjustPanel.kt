package com.ai.vis.ui.components

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.vis.R

data class AdjustParameter(
    val nameRes: Int,
    val iconRes: Int,
    var value: Float = 0f
)

@Composable
fun AdjustPanel(
    adjustmentValues: Map<Int, Float> = mapOf(),
    onValueChange: (Int, Float) -> Unit = { _, _ -> },
    onValueChangeStarted: (Int) -> Unit = { _ -> },
    onValueChangeFinished: (Int) -> Unit = { _ -> },
    modifier: Modifier = Modifier
) {
    val adjustParameters = listOf(
        AdjustParameter(R.string.brightness, R.drawable.ic_brightness),
        AdjustParameter(R.string.contrast, R.drawable.ic_contrast),
        AdjustParameter(R.string.saturation, R.drawable.ic_palette),
        AdjustParameter(R.string.sharpness, R.drawable.ic_high_quality),
        AdjustParameter(R.string.temperature, R.drawable.ic_temperature),
        AdjustParameter(R.string.tint, R.drawable.ic_palette)
    )
    
    var selectedParameterIndex by remember { mutableStateOf<Int?>(null) }
    val selectedParameter = selectedParameterIndex?.let { adjustParameters[it] }
    var isAdjusting by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Slider (shown when parameter is selected)
        AnimatedVisibility(
            visible = selectedParameterIndex != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            selectedParameterIndex?.let { index ->
                val param = adjustParameters[index]
                val currentValue = adjustmentValues[index] ?: 0f
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = param.nameRes),
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(currentValue * 100).toInt()}",
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = currentValue,
                        onValueChange = { newValue ->
                            if (!isAdjusting) {
                                isAdjusting = true
                                onValueChangeStarted(index)
                            }
                            onValueChange(index, newValue)
                        },
                        onValueChangeFinished = {
                            isAdjusting = false
                            onValueChangeFinished(index)
                        },
                        valueRange = if (index == 6) 0f..1f else -1f..1f, // Vignette 0-1, others -1 to 1
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                    )
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
            items(adjustParameters.size) { index ->
                val param = adjustParameters[index]
                AdjustParameterItem(
                    parameter = param,
                    isSelected = selectedParameterIndex == index,
                    onClick = {
                        selectedParameterIndex = if (selectedParameterIndex == index) null else index
                    }
                )
            }
        }
    }
}

@Composable
fun AdjustParameterItem(
    parameter: AdjustParameter,
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
                com.ai.vis.ui.theme.SelectionLightBlue
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
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(id = parameter.nameRes),
                    fontSize = 9.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 10.sp
                )
            }
        }
    }
}
