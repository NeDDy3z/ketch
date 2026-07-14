package com.neddy.ketch.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.ketch.di.AppContainer
import com.neddy.ketch.domain.ConnectionSelector
import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.Watcher
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WatcherConnection(
    val watcher: Watcher,
    val connection: TransitConnection?,
    val error: String?,
)

data class HomeUiState(
    val loading: Boolean = true,
    val watcherConnections: List<WatcherConnection> = emptyList(),
    val hasWatchers: Boolean = true,
    val missingApiKey: Boolean = false,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)

            val watchers = container.watcherRepository.getWatchers().filter { it.enabled }
            if (watchers.isEmpty()) {
                _uiState.value = HomeUiState(loading = false, hasWatchers = false)
                return@launch
            }

            val apiKey = container.settingsRepository.current().apiKey
            if (apiKey.isBlank()) {
                _uiState.value = HomeUiState(
                    loading = false,
                    hasWatchers = true,
                    missingApiKey = true,
                )
                return@launch
            }

            val location = container.locationProvider.currentLocation()
            val ordered = orderByProximity(watchers, location)

            val results = ordered.map { watcher ->
                async { lookup(watcher) }
            }.awaitAll()

            _uiState.value = HomeUiState(
                loading = false,
                watcherConnections = results,
                hasWatchers = true,
            )
        }
    }

    private suspend fun lookup(watcher: Watcher): WatcherConnection = try {
        val connections = container.transitRepository.findConnections(
            origin = watcher.origin,
            destination = watcher.destination,
            departureTime = Instant.now(),
        )
        val best = ConnectionSelector.selectBest(
            connections,
            maxTransfers = watcher.maxTransfers,
            maxTravelMinutes = watcher.maxTravelMinutes,
        )
        WatcherConnection(
            watcher = watcher,
            connection = best,
            error = if (best == null) "No connection found" else null,
        )
    } catch (e: Exception) {
        WatcherConnection(
            watcher = watcher,
            connection = null,
            error = e.message ?: "Lookup failed",
        )
    }

    /**
     * Watchers whose trigger location is closest to the device come first,
     * so the most relevant commute is on top.
     */
    private fun orderByProximity(watchers: List<Watcher>, location: Location?): List<Watcher> {
        if (location == null) return watchers
        return watchers.sortedBy { watcher ->
            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude,
                location.longitude,
                watcher.triggerLatitude,
                watcher.triggerLongitude,
                results,
            )
            results[0]
        }
    }
}
