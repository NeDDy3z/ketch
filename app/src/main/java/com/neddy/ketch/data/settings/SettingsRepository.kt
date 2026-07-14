package com.neddy.ketch.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.neddy.ketch.BuildConfig
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Defaults applied when creating a new watcher.
 */
data class WatcherDefaults(
    val activeDays: Set<DayOfWeek>,
    val windowStartMinutes: Int,
    val windowEndMinutes: Int,
    val triggerRadiusMeters: Int,
    val maxTransfers: Int?,
    val maxTravelMinutes: Int?,
)

data class AppSettings(
    val themeMode: ThemeMode,
    val apiKey: String,
    val watcherDefaults: WatcherDefaults,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val API_KEY = stringPreferencesKey("api_key")
        val DEFAULT_DAYS = stringPreferencesKey("default_days")
        val DEFAULT_WINDOW_START = intPreferencesKey("default_window_start")
        val DEFAULT_WINDOW_END = intPreferencesKey("default_window_end")
        val DEFAULT_RADIUS = intPreferencesKey("default_radius")
        val DEFAULT_MAX_TRANSFERS = intPreferencesKey("default_max_transfers")
        val DEFAULT_MAX_TRAVEL_MINUTES = intPreferencesKey("default_max_travel_minutes")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            apiKey = effectiveApiKey(prefs[Keys.API_KEY]),
            watcherDefaults = WatcherDefaults(
                activeDays = prefs[Keys.DEFAULT_DAYS]
                    ?.split(',')
                    ?.filter { it.isNotBlank() }
                    ?.map { DayOfWeek.of(it.trim().toInt()) }
                    ?.toSet()
                    ?: WEEKDAYS,
                windowStartMinutes = prefs[Keys.DEFAULT_WINDOW_START] ?: DEFAULT_WINDOW_START,
                windowEndMinutes = prefs[Keys.DEFAULT_WINDOW_END] ?: DEFAULT_WINDOW_END,
                triggerRadiusMeters = prefs[Keys.DEFAULT_RADIUS] ?: DEFAULT_RADIUS_METERS,
                maxTransfers = prefs[Keys.DEFAULT_MAX_TRANSFERS]?.takeIf { it >= 0 },
                maxTravelMinutes = prefs[Keys.DEFAULT_MAX_TRAVEL_MINUTES]?.takeIf { it > 0 },
            ),
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[Keys.API_KEY] = key.trim() }
    }

    suspend fun setWatcherDefaults(defaults: WatcherDefaults) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_DAYS] =
                defaults.activeDays.map { it.value }.sorted().joinToString(",")
            prefs[Keys.DEFAULT_WINDOW_START] = defaults.windowStartMinutes
            prefs[Keys.DEFAULT_WINDOW_END] = defaults.windowEndMinutes
            prefs[Keys.DEFAULT_RADIUS] = defaults.triggerRadiusMeters
            prefs[Keys.DEFAULT_MAX_TRANSFERS] = defaults.maxTransfers ?: -1
            prefs[Keys.DEFAULT_MAX_TRAVEL_MINUTES] = defaults.maxTravelMinutes ?: -1
        }
    }

    private fun effectiveApiKey(stored: String?): String =
        stored?.takeIf { it.isNotBlank() } ?: BuildConfig.MAPS_API_KEY

    companion object {
        val WEEKDAYS: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
        const val DEFAULT_WINDOW_START = 7 * 60
        const val DEFAULT_WINDOW_END = 9 * 60
        const val DEFAULT_RADIUS_METERS = 150
    }
}
