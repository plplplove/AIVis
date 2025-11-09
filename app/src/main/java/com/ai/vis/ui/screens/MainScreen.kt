package com.ai.vis.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import com.ai.vis.R
import com.ai.vis.ui.components.AIVisAppBar
import com.ai.vis.ui.components.ImageSourceBottomSheet
import com.ai.vis.ui.theme.AIVisTheme
import kotlinx.coroutines.launch

enum class NavigationTab(val titleRes: Int, val icon: Int) {
    EDITOR(R.string.editor, R.drawable.ic_home),
    GALLERY(R.string.my_gallery, R.drawable.ic_photo_collection)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSettingsClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AIVisAppBar(onSettingsClick = onSettingsClick)
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                painter = painterResource(id = tab.icon),
                                contentDescription = stringResource(id = tab.titleRes)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(id = tab.titleRes),
                                fontFamily = FontFamily(Font(R.font.font_main_text))
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> EditorScreen(
                onStartEditingClick = {
                    showBottomSheet = true
                },
                modifier = Modifier.padding(paddingValues)
            )
            1 -> MyGalleryScreen(
                modifier = Modifier.padding(paddingValues)
            )
        }

        // Bottom sheet for image source selection
        if (showBottomSheet) {
            ImageSourceBottomSheet(
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                },
                onGalleryClick = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                    onGalleryClick()
                },
                onCameraClick = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                    onCameraClick()
                },
                sheetState = sheetState
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AIVisTheme {
        MainScreen()
    }
}
