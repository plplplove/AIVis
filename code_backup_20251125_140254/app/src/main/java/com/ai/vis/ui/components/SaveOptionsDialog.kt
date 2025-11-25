package com.ai.vis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ai.vis.R

enum class SaveLocation {
    GALLERY_ONLY,
    APP_ONLY,
    BOTH
}

@Composable
fun SaveOptionsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaveLocationSelected: (SaveLocation) -> Unit
) {
    if (visible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 300f
                    )
                ),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(200)
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Title
                        Text(
                            text = stringResource(id = R.string.save_photo),
                            fontSize = 24.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        // Message
                        Text(
                            text = stringResource(id = R.string.save_options_message),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        
                        // Save options
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Save to Gallery
                            SaveOptionCard(
                                icon = R.drawable.ic_photo_collection,
                                title = stringResource(id = R.string.save_to_gallery),
                                description = stringResource(id = R.string.save_to_gallery_desc),
                                onClick = {
                                    onSaveLocationSelected(SaveLocation.GALLERY_ONLY)
                                }
                            )
                            
                            // Save to App
                            SaveOptionCard(
                                icon = R.drawable.ic_gallery,
                                title = stringResource(id = R.string.save_to_app),
                                description = stringResource(id = R.string.save_to_app_desc),
                                onClick = {
                                    onSaveLocationSelected(SaveLocation.APP_ONLY)
                                }
                            )
                            
                            // Save to Both
                            SaveOptionCard(
                                icon = R.drawable.ic_save,
                                title = stringResource(id = R.string.save_to_both),
                                description = stringResource(id = R.string.save_to_both_desc),
                                onClick = {
                                    onSaveLocationSelected(SaveLocation.BOTH)
                                },
                                isHighlighted = true
                            )
                        }
                        
                        // Cancel button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.cancel),
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveOptionCard(
    icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    isHighlighted: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = if (isHighlighted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
