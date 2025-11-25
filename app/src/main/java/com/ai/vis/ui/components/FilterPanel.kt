package com.ai.vis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.vis.R

enum class FilterType {
    NONE,
    BW,
    SEPIA,
    VINTAGE,
    COOL,
    WARM,
    GRAYSCALE,
    INVERT
}

data class Filter(
    val type: FilterType,
    val nameRes: Int,
    val color: Color 
)

@Composable
fun FilterPanel(
    currentFilter: FilterType,
    filterIntensity: Float,
    onFilterChange: (FilterType) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onIntensityChangeStarted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        Filter(FilterType.NONE, R.string.filter_none, Color(0xFFFFFFFF)),
        Filter(FilterType.BW, R.string.filter_bw, Color(0xFF808080)),
        Filter(FilterType.SEPIA, R.string.filter_sepia, Color(0xFFB58863)),
        Filter(FilterType.VINTAGE, R.string.filter_vintage, Color(0xFFD4A574)),
        Filter(FilterType.COOL, R.string.filter_cool, Color(0xFF6BA3D4)),
        Filter(FilterType.WARM, R.string.filter_warm, Color(0xFFE8A05D)),
        Filter(FilterType.GRAYSCALE, R.string.filter_grayscale, Color(0xFF999999)),
        Filter(FilterType.INVERT, R.string.filter_invert, Color(0xFF000000))
    )
    
    var isAdjustingIntensity by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = currentFilter != FilterType.NONE,
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
                        text = stringResource(id = R.string.filter_intensity),
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(filterIntensity * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = filterIntensity,
                    onValueChange = { newValue ->
                        if (!isAdjustingIntensity) {
                            isAdjustingIntensity = true
                            onIntensityChangeStarted()
                        }
                        onIntensityChange(newValue)
                    },
                    onValueChangeFinished = {
                        isAdjustingIntensity = false
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                )
            }
        }
        
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filters.size) { index ->
                val filter = filters[index]
                FilterItem(
                    filter = filter,
                    isSelected = currentFilter == filter.type,
                    onClick = {
                        onFilterChange(filter.type)
                    }
                )
            }
        }
    }
}

@Composable
fun FilterItem(
    filter: Filter,
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
                    com.ai.vis.ui.theme.SelectionColor()
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (filter.type == FilterType.INVERT) {
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black, Color.White)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(filter.color.copy(alpha = 0.5f), filter.color)
                                )
                            }
                        )
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(id = filter.nameRes),
                    fontSize = 12.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
