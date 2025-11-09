package com.ai.vis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.vis.R
import com.ai.vis.ui.theme.AIVisTheme

@Composable
fun MyGalleryScreen(
    modifier: Modifier = Modifier
) {
    var isSelectionMode by remember { mutableStateOf(false) }
    // TODO: Replace with actual gallery data
    val galleryImages = remember { emptyList<String>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar with "Select multiple" button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (galleryImages.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No photos yet",
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(R.font.font_title)),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Start editing to save your photos here",
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                Column {
                    // Selection mode toggle
                    TextButton(
                        onClick = { isSelectionMode = !isSelectionMode },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (isSelectionMode) "Cancel" else "Select multiple",
                            fontFamily = FontFamily(Font(R.font.font_main_text)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Grid of images (3 columns)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // TODO: Add gallery items here
                        // items(galleryImages.size) { index ->
                        //     GalleryImageCard(...)
                        // }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MyGalleryScreenPreview() {
    AIVisTheme {
        MyGalleryScreen()
    }
}
