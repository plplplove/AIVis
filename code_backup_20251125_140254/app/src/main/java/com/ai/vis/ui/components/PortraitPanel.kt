package com.ai.vis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

/**
 * Portrait AI panel with three options:
 * - Beauty Mode (general retouching)
 * - Eye Enhancement (brighten/enhance eyes)
 * - Face Blur (blur face area)
 */
enum class PortraitOption {
    NONE,
    BEAUTY_MODE,
    EYE_ENHANCEMENT,
    FACE_BLUR
}

data class PortraitItem(
    val option: PortraitOption,
    val nameRes: Int,
    val iconRes: Int
)

@Composable
fun PortraitPanel(
    selectedOption: PortraitOption,
    onOptionSelected: (PortraitOption) -> Unit,
    isProcessing: Boolean = false,
    beautyIntensity: Float = 0.5f,
    onBeautyIntensityChange: (Float) -> Unit = {},
    eyeIntensity: Float = 0.5f,
    onEyeIntensityChange: (Float) -> Unit = {},
    blurIntensity: Float = 0.5f,
    onBlurIntensityChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val itemsList = listOf(
        PortraitItem(PortraitOption.BEAUTY_MODE, R.string.beauty_mode, R.drawable.ic_face_retouch),
        PortraitItem(PortraitOption.EYE_ENHANCEMENT, R.string.eye_enhancement, R.drawable.ic_eye_enchasment),
        PortraitItem(PortraitOption.FACE_BLUR, R.string.face_blur, R.drawable.ic_blur)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Slider for Beauty Mode intensity
        AnimatedVisibility(
            visible = selectedOption == PortraitOption.BEAUTY_MODE,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
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
                        text = "Beauty Intensity",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(beautyIntensity * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = beautyIntensity,
                    onValueChange = onBeautyIntensityChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                )
            }
        }
        
        // Slider for Eye Enhancement intensity
        AnimatedVisibility(
            visible = selectedOption == PortraitOption.EYE_ENHANCEMENT,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
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
                        text = "Eye Brightness",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(eyeIntensity * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = eyeIntensity,
                    onValueChange = onEyeIntensityChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                )
            }
        }
        
        // Slider for Face Blur intensity
        AnimatedVisibility(
            visible = selectedOption == PortraitOption.FACE_BLUR,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
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
                        text = "Blur Intensity",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(blurIntensity * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = blurIntensity,
                    onValueChange = onBlurIntensityChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                )
            }
        }
        
        // Options Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsList.forEach { item ->
                val isSelected = selectedOption == item.option
                Box(modifier = Modifier.weight(1f)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        onClick = { 
                            if (!isProcessing) {
                                onOptionSelected(item.option)
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) com.ai.vis.ui.theme.SelectionColor() else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProcessing && isSelected) {
                                // Show loading indicator when processing
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = item.iconRes),
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(id = item.nameRes),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
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
