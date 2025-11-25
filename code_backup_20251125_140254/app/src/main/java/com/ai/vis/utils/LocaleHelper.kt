package com.ai.vis.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

object LocaleHelper {
    
    fun getLocale(languageCode: String): Locale {
        return when (languageCode) {
            "pl" -> Locale("pl")
            else -> Locale("en")
        }
    }
    
    fun applyLocale(context: Context, languageCode: String) {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
    
    fun createLocaleContext(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
}
