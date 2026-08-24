package com.hora.jnana

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("hora_prefs")

class DataStoreManager(private val context: Context) {
    companion object {
        val KEY_LAT = doublePreferencesKey("key_lat")
        val KEY_LON = doublePreferencesKey("key_lon")
        val KEY_LOCATION_NAME = stringPreferencesKey("key_location_name")
        val KEY_LOCATION_MODE = stringPreferencesKey("key_location_mode") // "gps" or "manual"
        val KEY_API_BASE = stringPreferencesKey("key_api_base")
        val KEY_LANG = stringPreferencesKey("key_lang")
        val KEY_THEME = stringPreferencesKey("key_theme")
        val KEY_THEME_MODE = stringPreferencesKey("key_theme_mode")
        val KEY_DASHA_LEVEL = androidx.datastore.preferences.core.intPreferencesKey("key_dasha_level")
        val KEY_SAVE_PATH = stringPreferencesKey("key_save_path")
        val KEY_CHART_STYLE = stringPreferencesKey("key_chart_style") // "north", "south", "east"
        val KEY_PRIVACY_ACCEPTED = androidx.datastore.preferences.core.booleanPreferencesKey("key_privacy_accepted")
        val KEY_TUTORIAL_SHOWN = androidx.datastore.preferences.core.booleanPreferencesKey("key_tutorial_shown")
        val KEY_CUSTOM_THEME_COLOR = stringPreferencesKey("key_custom_theme_color")
        val KEY_RECENT_COLORS = stringPreferencesKey("key_recent_colors")
    }

    val locationFlow: Flow<Pair<Double, Double>?> = context.dataStore.data.map { prefs ->
        val lat = prefs[KEY_LAT]
        val lon = prefs[KEY_LON]
        if (lat != null && lon != null) lat to lon else null
    }

    val locationNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOCATION_NAME]
    }

    val locationModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOCATION_MODE] ?: "gps"
    }

    val langFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANG] ?: "en"
    }

    val apiBaseFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val saved = prefs[KEY_API_BASE]
        // If the saved URL is the old production one, and it differs from current BuildConfig, use BuildConfig
        if (saved == "https://ndaskka.pythonanywhere.com/" && saved != BuildConfig.BASE_URL) {
            BuildConfig.BASE_URL
        } else {
            saved ?: BuildConfig.BASE_URL
        }
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "green"
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "light"
    }

    val dashaLevelFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DASHA_LEVEL] ?: 3
    }

    val savePathFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVE_PATH]
    }

    val chartStyleFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CHART_STYLE] ?: "south"
    }

    val privacyAcceptedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRIVACY_ACCEPTED] ?: false
    }

    val tutorialShownFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_TUTORIAL_SHOWN] ?: false
    }

    val customThemeColorFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_THEME_COLOR]
    }

    val recentColorsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_RECENT_COLORS]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    suspend fun saveLocation(lat: Double, lon: Double, name: String? = null, mode: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAT] = lat
            prefs[KEY_LON] = lon
            if (name != null) prefs[KEY_LOCATION_NAME] = name
            if (mode != null) prefs[KEY_LOCATION_MODE] = mode
        }
    }

    suspend fun saveLocationMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOCATION_MODE] = mode
        }
    }

    suspend fun getApiBase(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_API_BASE]
    }

    suspend fun saveApiBase(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_BASE] = url
        }
    }

    suspend fun getLang(): String {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_LANG] ?: "en"
    }

    suspend fun saveLang(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANG] = lang
        }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun saveDashaLevel(level: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DASHA_LEVEL] = level
        }
    }

    suspend fun saveSavePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVE_PATH] = path
        }
    }

    suspend fun saveChartStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CHART_STYLE] = style
        }
    }

    suspend fun savePrivacyAccepted(accepted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PRIVACY_ACCEPTED] = accepted
        }
    }

    suspend fun saveTutorialShown(shown: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TUTORIAL_SHOWN] = shown
        }
    }

    suspend fun saveCustomThemeColor(hex: String?) {
        context.dataStore.edit { prefs ->
            if (hex == null) prefs.remove(KEY_CUSTOM_THEME_COLOR)
            else prefs[KEY_CUSTOM_THEME_COLOR] = hex
        }
    }

    suspend fun saveRecentColors(colors: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RECENT_COLORS] = colors.joinToString(",")
        }
    }

    suspend fun resetSettings() {
        context.dataStore.edit { prefs ->
            val privacyAccepted = prefs[KEY_PRIVACY_ACCEPTED] ?: false
            prefs.clear()
            prefs[KEY_PRIVACY_ACCEPTED] = privacyAccepted
        }
    }
}
