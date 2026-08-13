package com.neddy.ketch.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.ketch.di.AppContainer
import com.neddy.ketch.domain.ConnectionSelector
import com.neddy.ketch.domain.JourneyPlanner
import com.neddy.ketch.domain.model.ParkedCar
import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.Watcher
import com.neddy.ketch.ui.components.userMessageFor
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val watcher: Watcher? = null,
    /** The connection the watcher would notify about right now. */
    val main: TransitConnection? = null,
    /** A later departure that travels faster, when one exists. */
    val quicker: TransitConnection? = null,
    val error: String? = null,
    val missing: Boolean = false,
    val deleted: Boolean = false,
    /** Where this journey was actually looked up to, car leg included. */
    val destinationName: String = "",
    val driveBefore: String? = null,
    val driveAfter: String? = null,
    /** Where the car is waiting, null when it is not out. */
    val parkedCar: ParkedCar? = null,
)

class WatcherDetailViewModel(
    private val container: AppContainer,
    private val watcherId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        _uiState.update { it.copy(refreshing = true) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val watcher = container.watcherRepository.getWatcher(watcherId)
            if (watcher == null) {
                _uiState.update {
                    it.copy(loading = false, refreshing = false, missing = true)
                }
                return@launch
            }
            _uiState.update { it.copy(watcher = watcher, loading = true, error = null) }

            val location = container.locationProvider.quickLocation()
            val parkedCar = container.settingsRepository.currentParkedCar()
                ?.takeIf { it.isOutAt(System.currentTimeMillis()) }
            val plan = JourneyPlanner.plan(
                watcher = watcher,
                latitude = location?.latitude,
                longitude = location?.longitude,
                speedKmh = null,
                carSpeedThresholdKmh = 0,
                parkedCar = parkedCar,
                now = System.currentTimeMillis(),
            )
            _uiState.update {
                it.copy(
                    parkedCar = parkedCar,
                    destinationName = plan.destination.name,
                    driveBefore = plan.driveBefore?.name,
                    driveAfter = plan.driveAfter?.name,
                )
            }
            try {
                val connections = container.transitRepository.findConnections(
                    origin = plan.origin,
                    destination = plan.destination,
                    departureTime = Instant.now(),
                )
                val main = ConnectionSelector.selectBest(
                    connections,
                    maxTransfers = watcher.maxTransfers,
                    maxTravelMinutes = watcher.maxTravelMinutes,
                    preferredVehicle = watcher.preferredVehicle,
                    maxTravelDeltaMinutes = watcher.maxTravelDeltaMinutes,
                )
                _uiState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        main = main,
                        quicker = main?.let { best ->
                            ConnectionSelector.selectQuickerAlternative(
                                connections = connections,
                                main = best,
                                maxTransfers = watcher.maxTransfers,
                                maxTravelMinutes = watcher.maxTravelMinutes,
                            )
                        },
                        error = if (main == null) {
                            "No connection found right now. Try adjusting the limits."
                        } else {
                            null
                        },
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        main = null,
                        quicker = null,
                        error = userMessageFor(e),
                    )
                }
            }
        }
    }

    /**
     * Declares the car out at this watcher's swap stop, or back home again.
     * Detection at the geofence can miss a slow pull-away, and this is the
     * override: it is the same state a detected drive writes, so both journeys
     * pick it up. Re-plans afterwards so the page reflects the new answer.
     */
    fun setCarOut(out: Boolean) {
        val stop = _uiState.value.watcher?.carStop ?: return
        viewModelScope.launch {
            container.settingsRepository.setParkedCar(
                if (out) {
                    ParkedCar(place = stop, parkedAt = System.currentTimeMillis())
                } else {
                    null
                },
            )
            refresh()
        }
    }

    fun delete() {
        val watcher = _uiState.value.watcher ?: return
        viewModelScope.launch {
            container.watcherRepository.delete(watcher)
            container.triggerSyncRequester.requestSync()
            _uiState.update { it.copy(deleted = true) }
        }
    }
}
