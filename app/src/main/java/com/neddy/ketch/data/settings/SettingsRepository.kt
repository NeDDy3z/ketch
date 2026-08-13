package com.neddy.ketch.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.neddy.ketch.BuildConfig
import com.neddy.ketch.domain.WalkAdjustment
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The dark tonal palette the whole app is tinted from. Ketch is dark-only, so
 * this replaces a light/dark choice entirely. [WALLPAPER] is the shipping
 * default on Android 12+ and takes its tones from the user's home screen; the
 * fixed seeds are the fallbacks, and guarantee a duration pill that survives a
 * pale wallpaper.
 */
enum class ColorPalette {
    WALLPAPER,
    STEEL,
    AURORA,
    PHOSPHOR,
    ICE_VIOLET,
    GRAPHITE,
    AMBER,
    MONO,
    ;

    companion object {
        val DEFAULT = STEEL
    }
}

/**
 * Which watchers a pull-to-refresh on the home screen looks up again.
 * [ALL] refreshes every enabled watcher; [ACTIVE] only refreshes watchers
 * whose active day and time window contain the current moment.
 */
enum class RefreshScope { ALL, ACTIVE }

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
    val palette: ColorPalette,
    val apiKey: String,
    val doubleTapOpensMaps: Boolean,
    val refreshScope: RefreshScope,
    val showResting: Boolean,
    /**
     * How much of the routing provider's walking time to shave off, in percent.
     * Zero leaves the provider's estimate alone.
     */
    val walkReductionPercent: Int,
    /** From this speed on, a leave counts as driving rather than walking. */
    val carSpeedThresholdKmh: Int,
    /** Whether Ketch watches its own GitHub releases for a newer build. */
    val updateChecksEnabled: Boolean,
    /** Epoch millis the update prompt stays quiet until, after a "Later". */
    val updateSnoozedUntil: Long,
    /** Epoch millis of the last release check, used to throttle the API. */
    val lastUpdateCheckAt: Long,
    val watcherDefaults: WatcherDefaults,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PALETTE = stringPreferencesKey("color_palette")
        val API_KEY = stringPreferencesKey("api_key")
        val DOUBLE_TAP_MAPS = booleanPreferencesKey("double_tap_opens_maps")
        val WALK_REDUCTION = intPreferencesKey("walk_reduction_percent")
        val CAR_SPEED_THRESHOLD = intPreferencesKey("car_speed_threshold_kmh")
        val UPDATE_CHECKS = booleanPreferencesKey("update_checks_enabled")
        val UPDATE_SNOOZED_UNTIL = longPreferencesKey("update_snoozed_until")
        val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check_at")
        val REFRESH_SCOPE = stringPreferencesKey("refresh_scope")
        val SHOW_RESTING = booleanPreferencesKey("show_resting")
        val DEFAULT_DAYS = stringPreferencesKey("default_days")
        val DEFAULT_WINDOW_START = intPreferencesKey("default_window_start")
        val DEFAULT_WINDOW_END = intPreferencesKey("default_window_end")
        val DEFAULT_RADIUS = intPreferencesKey("default_radius")
        val DEFAULT_MAX_TRANSFERS = intPreferencesKey("default_max_transfers")
        val DEFAULT_MAX_TRAVEL_MINUTES = intPreferencesKey("default_max_travel_minutes")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            palette = prefs[Keys.PALETTE]
                ?.let { runCatching { ColorPalette.valueOf(it) }.getOrNull() }
                ?: ColorPalette.DEFAULT,
            apiKey = effectiveApiKey(prefs[Keys.API_KEY]),
            doubleTapOpensMaps = prefs[Keys.DOUBLE_TAP_MAPS] ?: true,
            walkReductionPercent = (prefs[Keys.WALK_REDUCTION] ?: WalkAdjustment.DEFAULT_PERCENT)
                .coerceIn(0, WalkAdjustment.MAX_PERCENT),
            carSpeedThresholdKmh = (prefs[Keys.CAR_SPEED_THRESHOLD] ?: DEFAULT_CAR_SPEED_KMH)
                .coerceIn(MIN_CAR_SPEED_KMH, MAX_CAR_SPEED_KMH),
            updateChecksEnabled = prefs[Keys.UPDATE_CHECKS] ?: true,
            updateSnoozedUntil = prefs[Keys.UPDATE_SNOOZED_UNTIL] ?: 0L,
            lastUpdateCheckAt = prefs[Keys.LAST_UPDATE_CHECK] ?: 0L,
            refreshScope = prefs[Keys.REFRESH_SCOPE]
                ?.let { runCatching { RefreshScope.valueOf(it) }.getOrNull() }
                ?: RefreshScope.ALL,
            showResting = prefs[Keys.SHOW_RESTING] ?: true,
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

    suspend fun setPalette(palette: ColorPalette) {
        context.dataStore.edit { it[Keys.PALETTE] = palette.name }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[Keys.API_KEY] = key.trim() }
    }

    suspend fun setWalkReductionPercent(percent: Int) {
        context.dataStore.edit {
            it[Keys.WALK_REDUCTION] = percent.coerceIn(0, WalkAdjustment.MAX_PERCENT)
        }
    }

    suspend fun setCarSpeedThresholdKmh(kmh: Int) {
        context.dataStore.edit {
            it[Keys.CAR_SPEED_THRESHOLD] = kmh.coerceIn(MIN_CAR_SPEED_KMH, MAX_CAR_SPEED_KMH)
        }
    }

    suspend fun setDoubleTapOpensMaps(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DOUBLE_TAP_MAPS] = enabled }
    }

    suspend fun setRefreshScope(scope: RefreshScope) {
        context.dataStore.edit { it[Keys.REFRESH_SCOPE] = scope.name }
    }

    suspend fun setShowResting(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_RESTING] = show }
    }

    suspend fun setUpdateChecksEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.UPDATE_CHECKS] = enabled
            // Switching checks back on should ask again straight away.
            if (enabled) {
                prefs[Keys.UPDATE_SNOOZED_UNTIL] = 0L
                prefs[Keys.LAST_UPDATE_CHECK] = 0L
            }
        }
    }

    suspend fun setUpdateSnoozedUntil(timestamp: Long) {
        context.dataStore.edit { it[Keys.UPDATE_SNOOZED_UNTIL] = timestamp }
    }

    suspend fun setLastUpdateCheckAt(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_UPDATE_CHECK] = timestamp }
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

        /** Brisk cycling territory: past this nobody is on foot. */
        const val DEFAULT_CAR_SPEED_KMH = 15
        const val MIN_CAR_SPEED_KMH = 8
        const val MAX_CAR_SPEED_KMH = 40
    }
}
