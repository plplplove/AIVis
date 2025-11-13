package com.ai.vis.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.ai.vis.R
import com.ai.vis.data.EditedPhoto
import com.ai.vis.ui.components.ConfirmationDialog
import com.ai.vis.viewmodel.PhotoGalleryViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullImageViewerScreen(
    photo: EditedPhoto,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: PhotoGalleryViewModel = viewModel()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var currentFileName by remember { mutableStateOf(photo.fileName) }
    
    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    BackHandler(enabled = true) {
        onBackClick()
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentFileName,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { showEditNameDialog = true }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.navigationBars.asPaddingValues())
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Re-Edit Card
                    Card(
                        onClick = onReEditClick,
                        modifier = Modifier.weight(1f).height(80.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(id = R.string.re_edit),
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    // Export Card
                    Card(
                        onClick = { sharePhoto(context, photo, currentFileName) },
                        modifier = Modifier.weight(1f).height(80.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_download),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(id = R.string.export),
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.font_main_text)),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            // Full screen image with zoom and pan
            Image(
                painter = rememberAsyncImagePainter(
                    model = File(photo.filePath)
                ),
                contentDescription = currentFileName,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            
                            if (scale > 1f) {
                                val maxX = (size.width * (scale - 1)) / 2
                                val maxY = (size.height * (scale - 1)) / 2
                                offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
    
    // Edit Filename Dialog
    if (showEditNameDialog) {
        EditFilenameDialog(
            currentName = currentFileName,
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                if (newName.isNotBlank() && newName != currentFileName) {
                    currentFileName = newName
                    viewModel.updatePhotoFileName(photo.id, newName)
                    Toast.makeText(
                        context,
                        context.getString(R.string.filename_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showEditNameDialog = false
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.delete_photo),
            message = stringResource(id = R.string.delete_confirmation_message),
            confirmText = stringResource(id = R.string.delete),
            cancelText = stringResource(id = R.string.cancel),
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deletePhoto(photo)
                onDeleteClick()
            }
        )
    }
}

@Composable
private fun EditFilenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName.substringBeforeLast(".")) }
    val extension = currentName.substringAfterLast(".", "")
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = stringResource(id = R.string.edit_filename),
                    fontSize = 24.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                // Text input
                BasicTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                )
                
                if (extension.isNotEmpty()) {
                    Text(
                        text = ".$extension",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.font_main_text)),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                // Buttons
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
                        onClick = {
                            val fullName = if (extension.isNotEmpty()) "$newName.$extension" else newName
                            onConfirm(fullName)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.save),
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = stringResource(id = R.string.delete_photo),
                    fontSize = 24.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                // Message
                Text(
                    text = stringResource(id = R.string.delete_confirmation_message),
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.font_main_text)),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                
                // Buttons
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
                            text = stringResource(id = R.string.delete),
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

private fun sharePhoto(context: Context, photo: EditedPhoto, fileName: String) {
    try {
        val file = File(photo.filePath)
        if (!file.exists()) {
            Toast.makeText(
                context,
                "File not found",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Load bitmap from file
        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap == null) {
            Toast.makeText(
                context,
                "Failed to load image",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Save to gallery using the same method as PhotoEditor
        val uri = com.ai.vis.utils.PhotoSaver.saveToGallery(
            context = context,
            bitmap = bitmap,
            fileName = fileName
        )
        
        if (uri != null) {
            Toast.makeText(
                context,
                context.getString(R.string.photo_saved_to_gallery),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.error_saving_photo),
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(
            context,
            "Error: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}
