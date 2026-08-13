package com.neddy.ketch.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.ketch.data.settings.RefreshScope
import com.neddy.ketch.data.update.AppUpdate
import com.neddy.ketch.di.AppContainer
import com.neddy.ketch.domain.ConnectionSelector
import com.neddy.ketch.domain.OriginSelector
import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.Watcher
import com.neddy.ketch.ui.components.userMessageFor
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    /** Enabled, but outside its active day or time window right now. */
    val resting: Boolean = false,
) {
    val disabled: Boolean get() = !watcher.enabled
}

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val watcherConnections: List<WatcherConnection> = emptyList(),
    val hasWatchers: Boolean = true,
    val missingApiKey: Boolean = false,
    /** A newer release worth prompting about, null when there is nothing to say. */
    val availableUpdate: AppUpdate? = null,
    val doubleTapOpensMaps: Boolean = true,
    val showResting: Boolean = true,
) {
    /**
     * What the home list actually renders: active watchers first, then resting
     * and paused ones, each keeping the user's own order. Hidden entirely when
     * "Show resting" is off, so a morning list is only the morning commute.
     */
    val visibleWatcherConnections: List<WatcherConnection>
        get() {
            val (atRest, active) = watcherConnections.partition { it.resting || it.disabled }
            return if (showResting) active + atRest else active
        }

    /** True when there are watchers but the current filter hides all of them. */
    val allRestingHidden: Boolean
        get() = watcherConnections.isNotEmpty() && visibleWatcherConnections.isEmpty()
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** The last watcher list a full [load] ran against, for change detection. */
    private var loadedWatchers: List<Watcher>? = null

    /** Serializes reorder writes so overlapping commits persist in order. */
    private val reorderMutex = Mutex()

    /** The most recent deletion, held for the lifetime of its undo snackbar. */
    private var lastDeleted: List<Watcher> = emptyList()

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
                    _uiState.update {
                        it.copy(
                            doubleTapOpensMaps = settings.doubleTapOpensMaps,
                            showResting = settings.showResting,
                        )
                    }
                }
        }
        // Ketch is sideloaded, so nobody else is going to say a new build
        // exists. The check throttles and respects the user's last answer.
        viewModelScope.launch {
            val update = container.updateRepository.updateToPrompt()
            if (update != null) _uiState.update { it.copy(availableUpdate = update) }
        }
        // Resting is a function of the clock, not of the data, so nothing else
        // would notice a watcher's window opening. Re-evaluate on a slow tick so
        // cards wake up and re-sort on their own; this touches no network.
        viewModelScope.launch {
            while (true) {
                delay(RESTING_TICK_MS)
                _uiState.update { state ->
                    state.copy(
                        watcherConnections = state.watcherConnections.map {
                            it.copy(resting = it.watcher.isResting())
                        },
                    )
                }
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
                        byId[w.id]?.copy(watcher = w, resting = w.isResting())
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

    /**
     * The header sync icon and pull-to-refresh. Follows the refresh-scope
     * setting, so by default a morning commute never spends quota on an
     * evening watcher.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true) }
            val watchers = container.watcherRepository.getWatchers()
            loadedWatchers = watchers
            // When the user only wants active connections refreshed, skip
            // watchers whose active day and time window do not contain now.
            val refreshOnly: ((Watcher) -> Boolean)? =
                if (container.settingsRepository.current().refreshScope == RefreshScope.ACTIVE) {
                    { it.isActiveAt(LocalDateTime.now()) }
                } else {
                    null
                }
            load(watchers, refreshOnly)
            _uiState.update { it.copy(refreshing = false) }
        }
    }

    /**
     * The menu's "Refresh all": the only way to poll resting watchers, and one
     * deliberate step away from the cheap refresh because it costs the most
     * quota.
     */
    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true) }
            val watchers = container.watcherRepository.getWatchers()
            loadedWatchers = watchers
            load(watchers)
            _uiState.update { it.copy(refreshing = false) }
        }
    }

    /** "Later": gone for now, back tomorrow. */
    fun snoozeUpdate() {
        _uiState.update { it.copy(availableUpdate = null) }
        viewModelScope.launch { container.updateRepository.snooze() }
    }

    /** "Don't remind me again": gone until Settings turns checks back on. */
    fun dismissUpdatesForever() {
        _uiState.update { it.copy(availableUpdate = null) }
        viewModelScope.launch { container.updateRepository.disableChecks() }
    }

    /** The prompt has handed off to the browser, so it has done its job. */
    fun updateOpened() {
        _uiState.update { it.copy(availableUpdate = null) }
    }

    fun setShowResting(show: Boolean) {
        viewModelScope.launch { container.settingsRepository.setShowResting(show) }
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

    /**
     * Deletes immediately and keeps the rows aside so the snackbar can put them
     * back — faster for the common case than a confirmation dialog, and still
     * reversible. Saving restores the original ids and ordering, because the DAO
     * insert replaces on conflict.
     */
    fun delete(watchers: List<Watcher>) {
        if (watchers.isEmpty()) return
        lastDeleted = watchers
        viewModelScope.launch {
            watchers.forEach { container.watcherRepository.delete(it) }
            container.triggerSyncRequester.requestSync()
        }
    }

    fun undoDelete() {
        val restoring = lastDeleted
        if (restoring.isEmpty()) return
        lastDeleted = emptyList()
        viewModelScope.launch {
            restoring.forEach { container.watcherRepository.save(it) }
            container.triggerSyncRequester.requestSync()
        }
    }

    /**
     * Reloads connections. When [refreshOnly] is given, only watchers matching
     * it are looked up again; the rest keep whatever card state they had, so a
     * scoped refresh never clears connections it was told to leave alone.
     */
    private suspend fun load(
        watchers: List<Watcher>,
        refreshOnly: ((Watcher) -> Boolean)? = null,
    ) {
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

            fun shouldRefresh(watcher: Watcher): Boolean =
                watcher.enabled && !missingApiKey && (refreshOnly?.invoke(watcher) ?: true)

            val prior = _uiState.value.watcherConnections.associateBy { it.watcher.id }

            // Seed the list in the user order: watchers being refreshed start as
            // loading skeletons; the rest keep their prior connection so a
            // scoped refresh does not blank them out.
            _uiState.update {
                it.copy(
                    loading = false,
                    hasWatchers = true,
                    missingApiKey = missingApiKey,
                    watcherConnections = watchers.map { watcher ->
                        if (shouldRefresh(watcher)) {
                            WatcherConnection(
                                watcher = watcher,
                                connection = null,
                                error = null,
                                loading = true,
                                resting = watcher.isResting(),
                            )
                        } else {
                            val existing = prior[watcher.id]
                            WatcherConnection(
                                watcher = watcher,
                                connection = existing?.connection,
                                error = existing?.error,
                                loading = false,
                                resting = watcher.isResting(),
                            )
                        }
                    },
                )
            }

            if (missingApiKey) return@coroutineScope

            val location = container.locationProvider.quickLocation()
            watchers.forEachIndexed { index, watcher ->
                if (!shouldRefresh(watcher)) return@forEachIndexed
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
        // Browsing the list is not leaving, so a car start never applies here.
        val origin = OriginSelector.select(
            watcher = watcher,
            latitude = location?.latitude,
            longitude = location?.longitude,
            speedKmh = null,
            carSpeedThresholdKmh = 0,
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
            resting = watcher.isResting(),
        )
    } catch (e: Exception) {
        WatcherConnection(
            watcher = watcher,
            connection = null,
            error = userMessageFor(e),
            resting = watcher.isResting(),
        )
    }

    /** Enabled but outside its window right now — cheap to skip, still listed. */
    private fun Watcher.isResting(): Boolean =
        enabled && !isActiveAt(LocalDateTime.now())

    private companion object {
        /** A window boundary is minute-grained, so a minute of lag is invisible. */
        const val RESTING_TICK_MS = 60_000L
    }
}
