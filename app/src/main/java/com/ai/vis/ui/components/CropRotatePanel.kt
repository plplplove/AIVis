package com.ai.vis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.vis.R

enum class CropRatio(val nameRes: Int) {
    FREE(R.string.free_crop),
    RATIO_1_1(R.string.ratio_1_1),
    RATIO_4_3(R.string.ratio_4_3),
    RATIO_16_9(R.string.ratio_16_9)
}

@Composable
fun CropRotatePanel(
    currentStraightenAngle: Float = 0f,
    onCropRatioSelected: (CropRatio) -> Unit = {},
    onStraightenAngleChange: (Float) -> Unit = {},
    onRotateLeft: () -> Unit = {},
    onRotateRight: () -> Unit = {},
    onFlipHorizontal: () -> Unit = {},
    onFlipVertical: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedRatio by remember { mutableStateOf(CropRatio.FREE) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.crop),
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.font_main_text)),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CropRatio.entries.forEach { ratio ->
                CropRatioButton(
                    ratio = ratio,
                    isSelected = selectedRatio == ratio,
                    onClick = {
                        selectedRatio = ratio
                        onCropRatioSelected(ratio)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_straight),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.straighten),
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${currentStraightenAngle.toInt()}°",
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Slider(
                value = currentStraightenAngle,
                onValueChange = onStraightenAngleChange,
                valueRange = -10f..10f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolActionButton(
                iconRes = R.drawable.ic_rotate,
                labelRes = R.string.rotate_left,
                onClick = onRotateLeft,
                modifier = Modifier.weight(1f)
            )
            ToolActionButton(
                iconRes = R.drawable.ic_rotate,
                labelRes = R.string.rotate_right,
                onClick = onRotateRight,
                isFlipped = true,
                modifier = Modifier.weight(1f)
            )
            ToolActionButton(
                iconRes = R.drawable.ic_flip,
                labelRes = R.string.flip_horizontal,
                onClick = onFlipHorizontal,
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolActionButton(
                iconRes = R.drawable.ic_flip,
                labelRes = R.string.flip_vertical,
                onClick = onFlipVertical,
                isVertical = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun CropRatioButton(
    ratio: CropRatio,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    com.ai.vis.ui.theme.SelectionColor()
                else
                    MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = ratio.nameRes),
                fontSize = 13.sp,
                fontFamily = FontFamily(Font(R.font.font_main_text)),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ToolActionButton(
    iconRes: Int,
    labelRes: Int,
    onClick: () -> Unit,
    isFlipped: Boolean = false,
    isVertical: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(72.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (isFlipped) Modifier.graphicsLayer(scaleX = -1f)
                        else if (isVertical) Modifier.graphicsLayer(rotationZ = 90f)
                        else Modifier
                    )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = labelRes),
                fontSize = 9.sp,
                fontFamily = FontFamily(Font(R.font.font_main_text)),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 10.sp
            )
        }
    }
}
