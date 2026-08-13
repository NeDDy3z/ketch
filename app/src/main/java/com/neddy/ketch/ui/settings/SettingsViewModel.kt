package com.neddy.ketch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.ketch.data.settings.AppSettings
import com.neddy.ketch.data.settings.ColorPalette
import com.neddy.ketch.data.settings.RefreshScope
import com.neddy.ketch.data.settings.WatcherDefaults
import com.neddy.ketch.data.update.AppUpdate
import com.neddy.ketch.di.AppContainer
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the Updates card is showing right now. */
sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Available(val update: AppUpdate) : UpdateCheckState
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings?> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _updateCheck = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheck: StateFlow<UpdateCheckState> = _updateCheck.asStateFlow()

    /** The manual check ignores the throttle and the snooze: the user asked. */
    fun checkForUpdate() {
        if (_updateCheck.value == UpdateCheckState.Checking) return
        _updateCheck.value = UpdateCheckState.Checking
        viewModelScope.launch {
            val update = container.updateRepository.check()
            _updateCheck.value = if (update == null) {
                UpdateCheckState.UpToDate
            } else {
                UpdateCheckState.Available(update)
            }
        }
    }

    fun setUpdateChecksEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setUpdateChecksEnabled(enabled)
            if (!enabled) _updateCheck.value = UpdateCheckState.Idle
        }
    }

    fun setPalette(palette: ColorPalette) {
        viewModelScope.launch { container.settingsRepository.setPalette(palette) }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch { container.settingsRepository.setApiKey(key) }
    }

    fun setWalkReductionPercent(percent: Int) {
        viewModelScope.launch { container.settingsRepository.setWalkReductionPercent(percent) }
    }

    fun setCarSpeedThresholdKmh(kmh: Int) {
        viewModelScope.launch { container.settingsRepository.setCarSpeedThresholdKmh(kmh) }
    }

    fun setDoubleTapOpensMaps(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setDoubleTapOpensMaps(enabled) }
    }

    fun setRefreshScope(scope: RefreshScope) {
        viewModelScope.launch { container.settingsRepository.setRefreshScope(scope) }
    }

    fun toggleDefaultDay(day: DayOfWeek) {
        updateDefaults { defaults ->
            val days = if (day in defaults.activeDays) {
                defaults.activeDays - day
            } else {
                defaults.activeDays + day
            }
            defaults.copy(activeDays = days)
        }
    }

    fun setDefaultWindowStart(minutes: Int) {
        updateDefaults { it.copy(windowStartMinutes = minutes) }
    }

    fun setDefaultWindowEnd(minutes: Int) {
        updateDefaults { it.copy(windowEndMinutes = minutes) }
    }

    fun setDefaultRadius(meters: Int) {
        updateDefaults { it.copy(triggerRadiusMeters = meters) }
    }

    private fun updateDefaults(transform: (WatcherDefaults) -> WatcherDefaults) {
        viewModelScope.launch {
            val current = container.settingsRepository.current().watcherDefaults
            container.settingsRepository.setWatcherDefaults(transform(current))
        }
    }
}
