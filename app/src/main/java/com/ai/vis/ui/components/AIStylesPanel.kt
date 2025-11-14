package com.ai.vis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.vis.R
import com.ai.vis.domain.model.AIStyle

@Composable
fun AIStylesPanel(
    selectedStyle: AIStyle,
    isProcessing: Boolean,
    onStyleSelected: (AIStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep the outer container minimal to match FilterPanel (no extra background/padding)
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Horizontal list of style cards, matching FilterPanel spacing and padding
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(AIStyle.entries) { style ->
                AIStyleItem(
                    style = style,
                    isSelected = selectedStyle == style,
                    isProcessing = isProcessing && selectedStyle == style,
                    onClick = { onStyleSelected(style) }
                )
            }
        }
    }
}

@Composable
private fun getStyleDrawableRes(style: AIStyle): Int {
    // Return 0 for the NONE style so the caller can render a neutral placeholder
    // (the project doesn't include an explicit img_none drawable). All other
    // styles use the provided drawable assets.
    return when (style) {
        AIStyle.NONE -> 0
        AIStyle.OIL_PAINTING -> com.ai.vis.R.drawable.img_oil_painting
        AIStyle.WATERCOLOR -> com.ai.vis.R.drawable.img_watercolor
        AIStyle.CARTOON -> com.ai.vis.R.drawable.img_cartoon
        AIStyle.PENCIL_SKETCH -> com.ai.vis.R.drawable.img_pencil_sketch
        AIStyle.VAN_GOGH -> com.ai.vis.R.drawable.img_vangogh
        AIStyle.POP_ART -> com.ai.vis.R.drawable.img_pop_art
        AIStyle.IMPRESSIONISM -> com.ai.vis.R.drawable.img_impressionism
    }
}

@Composable
private fun AIStyleItem(
    style: AIStyle,
    isSelected: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(80.dp)
            .clickable(onClick = onClick),
        onClick = onClick,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected)
                com.ai.vis.ui.theme.SelectionColor()
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
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
                val resId = getStyleDrawableRes(style)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = stringResource(id = style.displayNameResId),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), androidx.compose.ui.graphics.Color.White)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }

                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(id = style.displayNameResId),
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

private fun getStyleEmoji(style: AIStyle): String {
    return when (style) {
        AIStyle.NONE -> "📷"
        AIStyle.OIL_PAINTING -> "🎨"
        AIStyle.WATERCOLOR -> "🖌️"
        AIStyle.CARTOON -> "🎭"
        AIStyle.PENCIL_SKETCH -> "✏️"
        AIStyle.VAN_GOGH -> "🌟"
        AIStyle.POP_ART -> "🎪"
        AIStyle.IMPRESSIONISM -> "🌸"
    }
}
