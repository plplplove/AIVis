package com.ai.vis.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val settings = AppSettings(
            isDarkTheme = preferences[IS_DARK_THEME] ?: false,
            selectedLanguage = preferences[SELECTED_LANGUAGE] ?: "English"
        )
        android.util.Log.d("SettingsDataStore", "Loading settings: $settings")
        settings
    }

    suspend fun updateTheme(isDarkTheme: Boolean) {
        android.util.Log.d("SettingsDataStore", "Updating theme to: $isDarkTheme")
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_THEME] = isDarkTheme
        }
        android.util.Log.d("SettingsDataStore", "Theme updated successfully")
    }

    suspend fun updateLanguage(language: String) {
        android.util.Log.d("SettingsDataStore", "Updating language to: $language")
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE] = language
        }
        android.util.Log.d("SettingsDataStore", "Language updated successfully")
    }

    suspend fun clearCache() {
        // TODO: Implement cache clearing logic
    }
}
