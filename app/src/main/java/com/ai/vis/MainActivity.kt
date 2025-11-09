package com.ai.vis

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.ai.vis.data.SettingsDataStore
import com.ai.vis.ui.screens.MainScreen
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
                var showSplash by remember { mutableStateOf(true) }
                var showSettings by remember { mutableStateOf(false) }
                
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
                    else -> {
                        MainScreen(
                            onSettingsClick = { showSettings = true }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    AIVisTheme {
        MainScreen()
    }
}