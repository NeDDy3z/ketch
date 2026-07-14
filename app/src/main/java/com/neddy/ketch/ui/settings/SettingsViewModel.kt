package com.neddy.ketch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.ketch.data.settings.AppSettings
import com.neddy.ketch.data.settings.ThemeMode
import com.neddy.ketch.data.settings.WatcherDefaults
import com.neddy.ketch.di.AppContainer
import java.time.DayOfWeek
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings?> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { container.settingsRepository.setThemeMode(mode) }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch { container.settingsRepository.setApiKey(key) }
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
