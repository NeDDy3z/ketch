package com.neddy.ketch.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.ketch.di.AppContainer
import com.neddy.ketch.domain.ConnectionSelector
import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.Watcher
import com.neddy.ketch.ui.components.userMessageFor
import java.time.Instant
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatcherConnection(
    val watcher: Watcher,
    val connection: TransitConnection?,
    val error: String?,
    val loading: Boolean = false,
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
        // Follow the database so creating, editing, toggling, or deleting a
        // watcher updates the home screen without a manual refresh.
        viewModelScope.launch {
            container.watcherRepository.observeWatchers()
                .map { list -> list.filter { it.enabled } }
                .distinctUntilChanged()
                .collectLatest { watchers -> load(watchers) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            load(container.watcherRepository.getWatchers().filter { it.enabled })
        }
    }

    private suspend fun load(watchers: List<Watcher>) {
        coroutineScope {
            _uiState.value = _uiState.value.copy(loading = true)

            if (watchers.isEmpty()) {
                _uiState.value = HomeUiState(loading = false, hasWatchers = false)
                return@coroutineScope
            }

            val apiKey = container.settingsRepository.current().apiKey
            if (apiKey.isBlank()) {
                _uiState.value = HomeUiState(
                    loading = false,
                    hasWatchers = true,
                    missingApiKey = true,
                )
                return@coroutineScope
            }

            val location = container.locationProvider.quickLocation()
            val ordered = orderByProximity(watchers, location)

            // Show one skeleton card per watcher right away and fill each in
            // as its lookup finishes, instead of waiting for the slowest.
            _uiState.value = HomeUiState(
                loading = false,
                hasWatchers = true,
                watcherConnections = ordered.map {
                    WatcherConnection(it, connection = null, error = null, loading = true)
                },
            )
            ordered.forEachIndexed { index, watcher ->
                launch {
                    val result = lookup(watcher, location)
                    _uiState.update { state ->
                        val connections = state.watcherConnections.toMutableList()
                        if (index < connections.size) connections[index] = result
                        state.copy(watcherConnections = connections)
                    }
                }
            }
        }
    }

    /**
     * Routes start at the current device position; without a fix, the
     * watcher trigger location stands in for it.
     */
    private suspend fun lookup(watcher: Watcher, location: Location?): WatcherConnection = try {
        val origin = StopPlace(
            name = "Current location",
            latitude = location?.latitude ?: watcher.triggerLatitude,
            longitude = location?.longitude ?: watcher.triggerLongitude,
        )
        val connections = container.transitRepository.findConnections(
            origin = origin,
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
            error = if (best == null) {
                "No connection found right now. Try adjusting the limits."
            } else {
                null
            },
        )
    } catch (e: Exception) {
        WatcherConnection(
            watcher = watcher,
            connection = null,
            error = userMessageFor(e),
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
