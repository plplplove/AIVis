package com.ai.vis

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import com.ai.vis.data.SettingsDataStore
import java.io.File
import com.ai.vis.ui.screens.MainScreen
import com.ai.vis.ui.screens.PhotoEditorScreen
import com.ai.vis.ui.screens.SettingsScreen
import com.ai.vis.ui.screens.SplashScreen
import com.ai.vis.ui.theme.AIVisTheme
import com.ai.vis.utils.LocaleHelper
import com.ai.vis.viewmodel.SettingsViewModel
import com.ai.vis.viewmodel.SettingsViewModelFactory
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val settingsDataStore by lazy { SettingsDataStore(applicationContext) }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(settingsDataStore)
    }
    
    override fun attachBaseContext(newBase: Context) {
        // Get saved language from DataStore synchronously is not possible here
        // So we'll apply locale in onCreate after collecting the flow
        super.attachBaseContext(newBase)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check if this is a recreate (e.g., after language change)
        val isRecreating = savedInstanceState != null
        
        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            var previousLanguage by remember { mutableStateOf(settings.selectedLanguage) }
            
            // Apply locale whenever language changes and recreate activity
            LaunchedEffect(settings.selectedLanguage) {
                LocaleHelper.applyLocale(this@MainActivity, settings.selectedLanguage)
                // Only recreate if language actually changed (not initial load)
                if (previousLanguage != settings.selectedLanguage && previousLanguage.isNotEmpty()) {
                    this@MainActivity.recreate()
                }
                previousLanguage = settings.selectedLanguage
            }
            
            AIVisTheme(darkTheme = settings.isDarkTheme) {
                // Don't show splash if this is a recreate (e.g., after language change)
                var showSplash by remember { mutableStateOf(!isRecreating) }
                var showSettings by remember { mutableStateOf(false) }
                var showPhotoEditor by remember { mutableStateOf(false) }
                var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
                var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
                
                // Gallery picker launcher
                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    android.util.Log.d("MainActivity", "Gallery returned URI: $uri")
                    uri?.let {
                        selectedImageUri = it
                        showPhotoEditor = true
                    }
                }
                
                // Camera launcher
                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    android.util.Log.d("MainActivity", "Camera success: $success, URI: $cameraImageUri")
                    if (success && cameraImageUri != null) {
                        selectedImageUri = cameraImageUri
                        showPhotoEditor = true
                    }
                }
                
                // Camera permission launcher
                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        // Launch camera after permission granted
                        val photoFile = File.createTempFile(
                            "camera_photo_${System.currentTimeMillis()}",
                            ".jpg",
                            this@MainActivity.cacheDir
                        )
                        val uri = FileProvider.getUriForFile(
                            this@MainActivity,
                            "${applicationContext.packageName}.fileprovider",
                            photoFile
                        )
                        cameraImageUri = uri
                        cameraLauncher.launch(uri)
                    }
                }
                
                when {
                    showSplash -> {
                        SplashScreen(
                            onTimeout = { showSplash = false }
                        )
                    }
                    showSettings -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBackClick = { showSettings = false }
                        )
                    }
                    showPhotoEditor -> {
                        PhotoEditorScreen(
                            imageUri = selectedImageUri,
                            onBackClick = { 
                                showPhotoEditor = false
                                selectedImageUri = null
                            }
                        )
                    }
                    else -> {
                        MainScreen(
                            onSettingsClick = { showSettings = true },
                            onGalleryClick = {
                                android.util.Log.d("MainActivity", "onGalleryClick called")
                                galleryLauncher.launch("image/*")
                            },
                            onCameraClick = {
                                android.util.Log.d("MainActivity", "onCameraClick called")
                                // Check camera permission first
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    when {
                                        checkSelfPermission(android.Manifest.permission.CAMERA) == 
                                            android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                                            // Permission already granted, launch camera
                                            val photoFile = File.createTempFile(
                                                "camera_photo_${System.currentTimeMillis()}",
                                                ".jpg",
                                                this@MainActivity.cacheDir
                                            )
                                            val uri = FileProvider.getUriForFile(
                                                this@MainActivity,
                                                "${applicationContext.packageName}.fileprovider",
                                                photoFile
                                            )
                                            cameraImageUri = uri
                                            cameraLauncher.launch(uri)
                                        }
                                        else -> {
                                            // Request permission
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    }
                                } else {
                                    // No permission needed for older versions
                                    val photoFile = File.createTempFile(
                                        "camera_photo_${System.currentTimeMillis()}",
                                        ".jpg",
                                        this@MainActivity.cacheDir
                                    )
                                    val uri = FileProvider.getUriForFile(
                                        this@MainActivity,
                                        "${applicationContext.packageName}.fileprovider",
                                        photoFile
                                    )
                                    cameraImageUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            },
                            onEditPhoto = { photoUri ->
                                // Handle re-edit from gallery
                                selectedImageUri = Uri.parse(photoUri)
                                showPhotoEditor = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Mark that we're saving state, so we know on recreate to skip splash
        outState.putBoolean("hasBeenCreated", true)
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    AIVisTheme {
        MainScreen()
    }
}