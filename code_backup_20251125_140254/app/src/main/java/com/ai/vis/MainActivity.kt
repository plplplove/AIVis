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
    private val photoRepository by lazy {
        val photoDao = com.ai.vis.data.AppDatabase.getDatabase(applicationContext).editedPhotoDao()
        com.ai.vis.data.PhotoRepository(photoDao)
    }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(settingsDataStore, photoRepository)
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val isRecreating = savedInstanceState != null
        
        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            var previousLanguage by remember { mutableStateOf(settings.selectedLanguage) }
            
            LaunchedEffect(settings.selectedLanguage) {
                LocaleHelper.applyLocale(this@MainActivity, settings.selectedLanguage)
                if (previousLanguage != settings.selectedLanguage && previousLanguage.isNotEmpty()) {
                    this@MainActivity.recreate()
                }
                previousLanguage = settings.selectedLanguage
            }
            
            AIVisTheme(darkTheme = settings.isDarkTheme) {
                var showSplash by remember { mutableStateOf(!isRecreating) }
                var showSettings by remember { mutableStateOf(false) }
                var showPhotoEditor by remember { mutableStateOf(false) }
                var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
                var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
                
                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        selectedImageUri = it
                        showPhotoEditor = true
                    }
                }
                
                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    if (success && cameraImageUri != null) {
                        selectedImageUri = cameraImageUri
                        showPhotoEditor = true
                    }
                }
                
                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
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
                                galleryLauncher.launch("image/*")
                            },
                            onCameraClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    when {
                                        checkSelfPermission(android.Manifest.permission.CAMERA) == 
                                            android.content.pm.PackageManager.PERMISSION_GRANTED -> {
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
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    }
                                } else {
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