package com.ai.vis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
 * Simple UI-only Background panel. Shows three options as cards:
 * - Remove Background
 * - Blur Background
 * - Replace Background
 *
 * This component does not implement processing; it exposes selection via onOptionSelected.
 */
enum class BackgroundOption {
    REMOVE,
    BLUR,
    REPLACE
}

data class BackgroundItem(
    val option: BackgroundOption,
    val nameRes: Int,
    val iconRes: Int
)

@Composable
fun BackgroundPanel(
    selectedOption: BackgroundOption,
    onOptionSelected: (BackgroundOption) -> Unit,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val itemsList = listOf(
        BackgroundItem(BackgroundOption.REMOVE, R.string.remove_background, R.drawable.ic_clear_bg),
        BackgroundItem(BackgroundOption.BLUR, R.string.blur_background, R.drawable.ic_blur),
        BackgroundItem(BackgroundOption.REPLACE, R.string.replace_background, R.drawable.ic_replace)
    )

    // Use a Row with equal weights so three cards span the full width evenly.
    Row(
        modifier = modifier
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
