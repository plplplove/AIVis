package com.ai.vis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ai.vis.R

@Composable
fun ExitConfirmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
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
                        Text(
                            text = stringResource(id = R.string.exit_without_saving),
                            fontSize = 24.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = stringResource(id = R.string.exit_confirmation_message),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.cancel),
                                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                                    fontSize = 16.sp
                                )
                            }
                            
                            OutlinedButton(
                                onClick = onConfirm,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.exit),
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
}
