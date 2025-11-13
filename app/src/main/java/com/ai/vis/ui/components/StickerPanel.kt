package com.ai.vis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.vis.R

@Composable
fun StickerPanel(
    currentSize: Float = 1.5f,
    currentOpacity: Float = 1f,
    onSizeChange: (Float) -> Unit = {},
    onOpacityChange: (Float) -> Unit = {},
    onStickerSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedParameter by remember { mutableIntStateOf(0) } // 0=Stickers, 1=Size, 2=Opacity
    
    // Popular emoji stickers organized by category
    val emojiCategories = mapOf(
        "Smileys" to listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃",
            "😉", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "☺️", "😚",
            "😙", "🥲", "😋", "😛", "😜", "🤪", "😝", "🤑"
        ),
        "Emotions" to listOf(
            "🤗", "🤭", "🤫", "🤔", "🤐", "🤨", "😐", "😑", "😶", "😏",
            "😒", "🙄", "😬", "🤥", "😌", "😔", "😪", "🤤", "😴"
        ),
        "Hearts" to listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
            "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟"
        ),
        "Hands" to listOf(
            "👍", "👎", "👊", "✊", "🤛", "🤜", "🤞", "✌️", "🤟", "🤘",
            "👌", "🤏", "👈", "👉", "👆", "👇", "☝️", "✋", "🤚", "🖐",
            "🖖", "👋", "🤙", "💪", "🙏"
        ),
        "Animals" to listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
            "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤", "🦄",
            "🐴", "🐝", "🐛", "🦋", "🐌", "🐞"
        ),
        "Food" to listOf(
            "🍎", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍈", "🍒", "🍑",
            "🍍", "🥝", "🥑", "🍅", "🍆", "🥕", "🌽", "🍞", "🥐", "🍕",
            "🍔", "🍟", "🌭", "🍗", "🍖", "🦴", "🍰", "🎂", "🍩", "🍪"
        ),
        "Objects" to listOf(
            "⭐", "🌟", "✨", "💫", "☀️", "🌙", "⚡", "🔥", "💥", "🎈",
            "🎉", "🎊", "🎁", "🏆", "🥇", "🥈", "🥉", "⚽", "🏀", "🏈",
            "⚾", "🎾", "🏐", "🎱", "🎯", "🎲"
        ),
        "Symbols" to listOf(
            "❌", "⭕", "✅", "✔️", "❗", "❓", "⚠️", "💯", "🔞", "🚫",
            "💢", "💤", "💨", "💦", "💭", "💬", "🗨️", "🗯️", "👁️", "🫶"
        )
    )

    var selectedCategory by remember { mutableStateOf(emojiCategories.keys.first()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Content based on selected parameter
        when (selectedParameter) {
            0 -> {
                // Category tabs for emojis
                ScrollableTabRow(
                    selectedTabIndex = emojiCategories.keys.indexOf(selectedCategory),
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    emojiCategories.keys.forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            text = {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Emoji grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(emojiCategories[selectedCategory] ?: emptyList()) { emoji ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onStickerSelected(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            1 -> {
                // Size slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .height(100.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Size",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = String.format("%.1fx", currentSize),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Slider(
                        value = currentSize,
                        onValueChange = onSizeChange,
                        valueRange = 0.5f..5f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            2 -> {
                // Opacity slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .height(100.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Opacity",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${(currentOpacity * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Slider(
                        value = currentOpacity,
                        onValueChange = onOpacityChange,
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
        
        // Parameter selector cards at bottom (matching TextPanel style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stickers card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                onClick = { selectedParameter = 0 },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedParameter == 0)
                        com.ai.vis.ui.theme.SelectionLightBlue
                    else MaterialTheme.colorScheme.surface
                ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectedParameter == 0) 4.dp else 2.dp
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
                                painter = painterResource(id = R.drawable.ic_sticker),
                                contentDescription = null,
                                tint = if (selectedParameter == 0)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Stickers",
                                fontSize = 12.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = if (selectedParameter == 0)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            
            // Size card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                onClick = { selectedParameter = 1 },
                colors = CardDefaults.cardColors(
                        containerColor = if (selectedParameter == 1)
                            com.ai.vis.ui.theme.SelectionLightBlue
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectedParameter == 1) 4.dp else 2.dp
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
                                painter = painterResource(id = R.drawable.ic_size),
                                contentDescription = null,
                                tint = if (selectedParameter == 1)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Size",
                                fontSize = 12.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = if (selectedParameter == 1)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            
            // Opacity card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                    onClick = { selectedParameter = 2 },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedParameter == 2)
                            com.ai.vis.ui.theme.SelectionLightBlue
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectedParameter == 2) 4.dp else 2.dp
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
                                painter = painterResource(id = R.drawable.ic_opacity),
                                contentDescription = null,
                                tint = if (selectedParameter == 2)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Opacity",
                                fontSize = 12.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = if (selectedParameter == 2)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
        }
    }
}
