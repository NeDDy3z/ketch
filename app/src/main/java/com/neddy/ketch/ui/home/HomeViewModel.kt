package com.neddy.ketch.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.ketch.data.settings.EditGesture
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class WatcherConnection(
    val watcher: Watcher,
    val connection: TransitConnection?,
    val error: String?,
    val loading: Boolean = false,
) {
    val disabled: Boolean get() = !watcher.enabled
}

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val watcherConnections: List<WatcherConnection> = emptyList(),
    val hasWatchers: Boolean = true,
    val missingApiKey: Boolean = false,
    val editGesture: EditGesture = EditGesture.TAP,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** The last watcher list a full [load] ran against, for change detection. */
    private var loadedWatchers: List<Watcher>? = null

    /** Serializes reorder writes so overlapping commits persist in order. */
    private val reorderMutex = Mutex()

    init {
        // Follow the database so creating, editing, reordering, or deleting a
        // watcher updates the home screen without a manual refresh. The order
        // is the user defined home order, ascending sortOrder.
        viewModelScope.launch {
            container.watcherRepository.observeWatchers()
                .collectLatest { watchers -> onWatchersChanged(watchers) }
        }
        viewModelScope.launch {
            container.settingsRepository.settings
                .collectLatest { settings ->
                    _uiState.update { it.copy(editGesture = settings.editGesture) }
                }
        }
    }

    /**
     * Reacts to a database emission. When only the ordering changed, the cards
     * are reshuffled in place so a reorder never re-runs the network lookups
     * or flashes skeletons; any other change triggers a full reload.
     */
    private suspend fun onWatchersChanged(watchers: List<Watcher>) {
        val previous = loadedWatchers
        if (previous != null && sameIgnoringOrder(previous, watchers)) {
            loadedWatchers = watchers
            _uiState.update { state ->
                val byId = state.watcherConnections.associateBy { it.watcher.id }
                state.copy(
                    watcherConnections = watchers.mapNotNull { w ->
                        byId[w.id]?.copy(watcher = w)
                    },
                )
            }
            return
        }
        loadedWatchers = watchers
        load(watchers)
    }

    private fun sameIgnoringOrder(a: List<Watcher>, b: List<Watcher>): Boolean {
        if (a.size != b.size) return false
        val normalizedA = a.map { it.copy(sortOrder = 0) }.sortedBy { it.id }
        val normalizedB = b.map { it.copy(sortOrder = 0) }.sortedBy { it.id }
        return normalizedA == normalizedB
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true) }
            val watchers = container.watcherRepository.getWatchers()
            loadedWatchers = watchers
            load(watchers)
            _uiState.update { it.copy(refreshing = false) }
        }
    }

    /** Persists a new home ordering after a drag reorder. */
    fun reorder(orderedIds: List<Long>) {
        // Reflect the new order locally right away so the list does not jump.
        _uiState.update { state ->
            val byId = state.watcherConnections.associateBy { it.watcher.id }
            state.copy(watcherConnections = orderedIds.mapNotNull { byId[it] })
        }
        viewModelScope.launch {
            reorderMutex.withLock { container.watcherRepository.reorder(orderedIds) }
        }
    }

    fun setEnabled(watcher: Watcher, enabled: Boolean) {
        if (watcher.enabled == enabled) return
        viewModelScope.launch {
            container.watcherRepository.save(watcher.copy(enabled = enabled))
            container.triggerSyncRequester.requestSync()
        }
    }

    fun delete(watchers: List<Watcher>) {
        if (watchers.isEmpty()) return
        viewModelScope.launch {
            watchers.forEach { container.watcherRepository.delete(it) }
            container.triggerSyncRequester.requestSync()
        }
    }

    private suspend fun load(watchers: List<Watcher>) {
        coroutineScope {
            _uiState.update { it.copy(loading = true) }

            if (watchers.isEmpty()) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        hasWatchers = false,
                        watcherConnections = emptyList(),
                        missingApiKey = false,
                    )
                }
                return@coroutineScope
            }

            val enabled = watchers.filter { it.enabled }
            val apiKey = container.settingsRepository.current().apiKey
            val missingApiKey = enabled.isNotEmpty() && apiKey.isBlank()

            // Seed the list in the user order: enabled watchers start as
            // loading skeletons, disabled ones render as a muted resting card.
            _uiState.update {
                it.copy(
                    loading = false,
                    hasWatchers = true,
                    missingApiKey = missingApiKey,
                    watcherConnections = watchers.map { watcher ->
                        WatcherConnection(
                            watcher = watcher,
                            connection = null,
                            error = null,
                            loading = watcher.enabled && !missingApiKey,
                        )
                    },
                )
            }

            if (missingApiKey) return@coroutineScope

            val location = container.locationProvider.quickLocation()
            watchers.forEachIndexed { index, watcher ->
                if (!watcher.enabled) return@forEachIndexed
                val result = lookup(watcher, location)
                _uiState.update { state ->
                    val connections = state.watcherConnections.toMutableList()
                    val at = connections.indexOfFirst { it.watcher.id == watcher.id }
                    if (at >= 0) connections[at] = result
                    state.copy(watcherConnections = connections)
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
            preferredVehicle = watcher.preferredVehicle,
            maxTravelDeltaMinutes = watcher.maxTravelDeltaMinutes,
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
}
