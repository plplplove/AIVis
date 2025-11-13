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
import androidx.compose.foundation.lazy.items
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

enum class TextFontFamily(val fontRes: Int, val nameRes: Int) {
    ABITE(R.font.abite, R.string.font_abite),
    AMERICAN_CAPTAIN(R.font.americancaptain, R.string.font_american_captain),
    BEAR_DAYS(R.font.bear_days, R.string.font_bear_days),
    COOL_VETICA(R.font.coolveticarg, R.string.font_cool_vetica),
    CUTE_NOTES(R.font.cutenotes, R.string.font_cute_notes),
    JMH_TYPEWRITER(R.font.jmhtypewriterlack, R.string.font_jmh_typewriter),
    KARINA(R.font.karina, R.string.font_karina),
    KEEP_ON_TRUCKIN(R.font.keepontruckin, R.string.font_keep_on_truckin),
    KOMICAX(R.font.komicax, R.string.font_komicax),
    LEMON_MILK(R.font.lemonmilk, R.string.font_lemon_milk),
    PORKYS(R.font.porkys, R.string.font_porkys),
    RIFFIC_FREE(R.font.rifficfree, R.string.font_riffic_free),
    ROSSTEN(R.font.rossten, R.string.font_rossten),
    SWEETY_RASTY(R.font.sweetyrasty, R.string.font_sweety_rasty),
    VARSITY_TEAM(R.font.varsityteam, R.string.font_varsity_team)
}

data class TextStyle(
    var text: String = "",
    var size: Float = 24f, // 16-72
    var fontFamily: TextFontFamily = TextFontFamily.LEMON_MILK,
    var color: Color = Color.White,
    var opacity: Float = 1f, // 0-1 прозорість тексту
    var alignment: TextAlignment = TextAlignment.CENTER,
    var weight: TextWeight = TextWeight.NORMAL,
    var letterSpacing: Float = 0f, // -5 to 10
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var isStrikethrough: Boolean = false,
    var hasStroke: Boolean = false,
    var hasBackground: Boolean = false,
    var backgroundOpacity: Float = 0.7f, // 0-1 прозорість фону
    var shadowRadius: Float = 0f, // 0-10 радіус тіні
    var shadowOffsetX: Float = 0f, // -10 до 10
    var shadowOffsetY: Float = 0f // -10 до 10
)

data class TextOption(
    val nameRes: Int,
    val iconRes: Int,
    val type: TextOptionType
)

enum class TextOptionType {
    SIZE, FONT, COLOR, OPACITY, ALIGNMENT, WEIGHT, LETTER_SPACING, DECORATIONS, BACKGROUND, SHADOW
}

@Composable
fun TextPanel(
    textStyle: TextStyle,
    onTextChange: (String) -> Unit = {},
    onSizeChange: (Float) -> Unit = {},
    onFontChange: (TextFontFamily) -> Unit = {},
    onColorChange: (Color) -> Unit = {},
    onOpacityChange: (Float) -> Unit = {},
    onAlignmentChange: (TextAlignment) -> Unit = {},
    onWeightChange: (TextWeight) -> Unit = {},
    onLetterSpacingChange: (Float) -> Unit = {},
    onItalicToggle: (Boolean) -> Unit = {},
    onUnderlineToggle: (Boolean) -> Unit = {},
    onStrikethroughToggle: (Boolean) -> Unit = {},
    onStrokeToggle: (Boolean) -> Unit = {},
    onBackgroundToggle: (Boolean) -> Unit = {},
    onBackgroundOpacityChange: (Float) -> Unit = {},
    onShadowChange: (Float, Float, Float) -> Unit = { _, _, _ -> }, // radius, offsetX, offsetY
    modifier: Modifier = Modifier
) {
    // Порядок: Size, Font, Color, Opacity, Weight, Letter Spacing, Decorations, Background, Shadow, Alignment
    val textOptions = listOf(
        TextOption(R.string.text_size, R.drawable.ic_text, TextOptionType.SIZE),
        TextOption(R.string.text_font, R.drawable.ic_text, TextOptionType.FONT),
        TextOption(R.string.text_color, R.drawable.ic_palette, TextOptionType.COLOR),
        TextOption(R.string.text_opacity, R.drawable.ic_opacity, TextOptionType.OPACITY),
        TextOption(R.string.font_weight, R.drawable.ic_text, TextOptionType.WEIGHT),
        TextOption(R.string.letter_spacing, R.drawable.ic_lette_spacing, TextOptionType.LETTER_SPACING),
        TextOption(R.string.text_italic, R.drawable.ic_italic, TextOptionType.DECORATIONS),
        TextOption(R.string.background, R.drawable.ic_background, TextOptionType.BACKGROUND),
        TextOption(R.string.shadow, R.drawable.ic_shadow, TextOptionType.SHADOW),
        TextOption(R.string.alignment, R.drawable.ic_align_center, TextOptionType.ALIGNMENT)
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
                            .height(100.dp) // Стандартна висота
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
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
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
                TextOptionType.FONT -> {
                    // Font picker with scrollable list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.text_font),
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(TextFontFamily.entries.size) { index ->
                                val font = TextFontFamily.entries[index]
                                FontCard(
                                    font = font,
                                    isSelected = textStyle.fontFamily == font,
                                    onClick = { onFontChange(font) }
                                )
                            }
                        }
                    }
                }
                TextOptionType.COLOR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(100.dp) // Стандартна висота
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
                TextOptionType.OPACITY -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(100.dp) // Стандартна висота
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.text_opacity),
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(textStyle.opacity * 100).toInt()}%",
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Slider(
                            value = textStyle.opacity,
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
                TextOptionType.BACKGROUND -> {
                    // Only show opacity slider if background is enabled
                    if (textStyle.hasBackground) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .height(100.dp) // Стандартна висота
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.background),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(textStyle.backgroundOpacity * 100).toInt()}%",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Slider(
                                value = textStyle.backgroundOpacity,
                                onValueChange = onBackgroundOpacityChange,
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
                TextOptionType.SHADOW -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Shadow Radius
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.shadow_radius),
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${textStyle.shadowRadius.toInt()}",
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Slider(
                            value = textStyle.shadowRadius,
                            onValueChange = { onShadowChange(it, textStyle.shadowOffsetX, textStyle.shadowOffsetY) },
                            valueRange = 0f..10f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Shadow Offset X
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.shadow_offset_x),
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${textStyle.shadowOffsetX.toInt()}",
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Slider(
                            value = textStyle.shadowOffsetX,
                            onValueChange = { onShadowChange(textStyle.shadowRadius, it, textStyle.shadowOffsetY) },
                            valueRange = -10f..10f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Shadow Offset Y
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.shadow_offset_y),
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${textStyle.shadowOffsetY.toInt()}",
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Slider(
                            value = textStyle.shadowOffsetY,
                            onValueChange = { onShadowChange(textStyle.shadowRadius, textStyle.shadowOffsetX, it) },
                            valueRange = -10f..10f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
                TextOptionType.LETTER_SPACING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(100.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.letter_spacing),
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${textStyle.letterSpacing.toInt()}",
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Slider(
                            value = textStyle.letterSpacing,
                            onValueChange = onLetterSpacingChange,
                            valueRange = -5f..10f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
                TextOptionType.DECORATIONS -> {
                    // Показуємо 3 toggle кнопки для italic, underline, strikethrough
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DecorationToggle(
                            iconRes = R.drawable.ic_italic,
                            labelRes = R.string.text_italic,
                            isEnabled = textStyle.isItalic,
                            onClick = { onItalicToggle(!textStyle.isItalic) },
                            modifier = Modifier.weight(1f)
                        )
                        DecorationToggle(
                            iconRes = R.drawable.ic_underline,
                            labelRes = R.string.text_underline,
                            isEnabled = textStyle.isUnderline,
                            onClick = { onUnderlineToggle(!textStyle.isUnderline) },
                            modifier = Modifier.weight(1f)
                        )
                        DecorationToggle(
                            iconRes = R.drawable.ic_cross,
                            labelRes = R.string.text_strikethrough,
                            isEnabled = textStyle.isStrikethrough,
                            onClick = { onStrikethroughToggle(!textStyle.isStrikethrough) },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                // Для Weight та Background показуємо поточний стан
                when (option.type) {
                    TextOptionType.WEIGHT -> {
                        WeightCard(
                            textStyle = textStyle,
                            onClick = { onWeightChange(textStyle.weight) }
                        )
                    }
                    TextOptionType.BACKGROUND -> {
                        BackgroundCard(
                            textStyle = textStyle,
                            isSelected = selectedOption == TextOptionType.BACKGROUND,
                            onClick = {
                                // Toggle background on/off
                                val newBackgroundState = !textStyle.hasBackground
                                onBackgroundToggle(newBackgroundState)
                                
                                // If enabling, show the opacity slider
                                if (newBackgroundState) {
                                    selectedOption = TextOptionType.BACKGROUND
                                } else {
                                    // If disabling, hide the slider
                                    selectedOption = null
                                }
                            }
                        )
                    }
                    else -> {
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
    }
}

@Composable
fun WeightCard(
    textStyle: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(80.dp)
            .height(80.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                // Велика "A" з поточною жирністю
                Text(
                    text = "A",
                    fontSize = 28.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = when (textStyle.weight) {
                        TextWeight.LIGHT -> FontWeight.Light
                        TextWeight.NORMAL -> FontWeight.Normal
                        TextWeight.BOLD -> FontWeight.Bold
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (textStyle.weight) {
                        TextWeight.LIGHT -> "Light"
                        TextWeight.NORMAL -> "Normal"
                        TextWeight.BOLD -> "Bold"
                    },
                    fontSize = 10.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun BackgroundCard(
    textStyle: TextStyle,
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
            // Checkbox icon - filled if enabled, empty if disabled
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                if (textStyle.hasBackground) {
                    // Filled checkbox
                    Icon(
                        painter = painterResource(id = R.drawable.ic_background),
                        contentDescription = null,
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(4.dp)
                    )
                } else {
                    // Empty checkbox (just border)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.background),
                fontSize = 10.sp,
                fontFamily = FontFamily(Font(R.font.font_main_text)),
                color = if (textStyle.hasBackground)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1
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
    Card(
        modifier = modifier
            .width(80.dp)
            .height(80.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && option.type != TextOptionType.WEIGHT && option.type != TextOptionType.BACKGROUND)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected && option.type != TextOptionType.WEIGHT && option.type != TextOptionType.BACKGROUND) 4.dp else 2.dp
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
                tint = if (isSelected && option.type != TextOptionType.WEIGHT && option.type != TextOptionType.BACKGROUND)
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
                color = if (isSelected && option.type != TextOptionType.WEIGHT && option.type != TextOptionType.BACKGROUND)
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
            .size(40.dp)
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
fun FontCard(
    font: TextFontFamily,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(100.dp)
            .height(80.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Show "Aa" in the font style
            Text(
                text = "Aa",
                fontSize = 28.sp,
                fontFamily = FontFamily(Font(font.fontRes)),
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
fun DecorationToggle(
    iconRes: Int,
    labelRes: Int,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(72.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isEnabled) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = labelRes),
                fontSize = 10.sp,
                fontFamily = FontFamily(Font(R.font.font_main_text)),
                color = if (isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
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
