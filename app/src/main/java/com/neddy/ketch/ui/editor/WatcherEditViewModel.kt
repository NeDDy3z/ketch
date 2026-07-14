package com.neddy.ketch.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.ketch.di.AppContainer
import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.TriggerType
import com.neddy.ketch.domain.model.Watcher
import java.time.DayOfWeek
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StopField { ORIGIN, DESTINATION }

data class EditUiState(
    val loading: Boolean = true,
    val name: String = "",
    val origin: StopPlace? = null,
    val destination: StopPlace? = null,
    val originQuery: String = "",
    val destinationQuery: String = "",
    val searchField: StopField? = null,
    val searchResults: List<StopPlace> = emptyList(),
    val searching: Boolean = false,
    val searchError: String? = null,
    val triggerType: TriggerType = TriggerType.LOCATION_EXIT,
    val triggerLatitude: Double? = null,
    val triggerLongitude: Double? = null,
    val triggerRadiusMeters: Int = 150,
    val activeDays: Set<DayOfWeek> = emptySet(),
    val windowStartMinutes: Int = 7 * 60,
    val windowEndMinutes: Int = 9 * 60,
    val notificationsEnabled: Boolean = true,
    val maxTransfersText: String = "",
    val maxTravelMinutesText: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val validationError: String? = null,
) {
    val canSave: Boolean
        get() = !saving && name.isNotBlank() && origin != null && destination != null &&
            activeDays.isNotEmpty()
}

class WatcherEditViewModel(
    private val container: AppContainer,
    private val watcherId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var editedWatcher: Watcher? = null

    init {
        viewModelScope.launch {
            if (watcherId != null) {
                loadExisting(watcherId)
            } else {
                loadDefaults()
            }
        }
    }

    private suspend fun loadExisting(id: Long) {
        val watcher = container.watcherRepository.getWatcher(id)
        if (watcher == null) {
            loadDefaults()
            return
        }
        editedWatcher = watcher
        _uiState.update {
            EditUiState(
                loading = false,
                name = watcher.name,
                origin = watcher.origin,
                destination = watcher.destination,
                originQuery = watcher.origin.name,
                destinationQuery = watcher.destination.name,
                triggerType = watcher.triggerType,
                triggerLatitude = watcher.triggerLatitude,
                triggerLongitude = watcher.triggerLongitude,
                triggerRadiusMeters = watcher.triggerRadiusMeters,
                activeDays = watcher.activeDays,
                windowStartMinutes = watcher.windowStartMinutes,
                windowEndMinutes = watcher.windowEndMinutes,
                notificationsEnabled = watcher.notificationsEnabled,
                maxTransfersText = watcher.maxTransfers?.toString().orEmpty(),
                maxTravelMinutesText = watcher.maxTravelMinutes?.toString().orEmpty(),
            )
        }
    }

    private suspend fun loadDefaults() {
        val defaults = container.settingsRepository.current().watcherDefaults
        _uiState.update {
            EditUiState(
                loading = false,
                triggerRadiusMeters = defaults.triggerRadiusMeters,
                activeDays = defaults.activeDays,
                windowStartMinutes = defaults.windowStartMinutes,
                windowEndMinutes = defaults.windowEndMinutes,
                maxTransfersText = defaults.maxTransfers?.toString().orEmpty(),
                maxTravelMinutesText = defaults.maxTravelMinutes?.toString().orEmpty(),
            )
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value) }

    fun setStopQuery(field: StopField, query: String) {
        _uiState.update {
            when (field) {
                StopField.ORIGIN -> it.copy(
                    originQuery = query,
                    origin = if (query == it.origin?.name) it.origin else null,
                    searchField = field,
                )
                StopField.DESTINATION -> it.copy(
                    destinationQuery = query,
                    destination = if (query == it.destination?.name) it.destination else null,
                    searchField = field,
                )
            }
        }
        searchJob?.cancel()
        if (query.length < 3) {
            _uiState.update { it.copy(searchResults = emptyList(), searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _uiState.update { it.copy(searching = true, searchError = null) }
            try {
                val results = container.transitRepository.searchStops(query)
                _uiState.update { it.copy(searchResults = results, searching = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
                        searching = false,
                        searchError = e.message ?: "Search failed",
                    )
                }
            }
        }
    }

    fun selectStop(field: StopField, stop: StopPlace) {
        _uiState.update {
            when (field) {
                StopField.ORIGIN -> it.copy(
                    origin = stop,
                    originQuery = stop.name,
                    searchField = null,
                    searchResults = emptyList(),
                )
                StopField.DESTINATION -> it.copy(
                    destination = stop,
                    destinationQuery = stop.name,
                    searchField = null,
                    searchResults = emptyList(),
                )
            }
        }
    }

    fun swapStops() {
        _uiState.update {
            it.copy(
                origin = it.destination,
                destination = it.origin,
                originQuery = it.destinationQuery,
                destinationQuery = it.originQuery,
            )
        }
    }

    fun setTriggerType(type: TriggerType) = _uiState.update { it.copy(triggerType = type) }

    fun setTriggerRadius(meters: Int) =
        _uiState.update { it.copy(triggerRadiusMeters = meters) }

    fun useCurrentLocationAsTrigger() {
        viewModelScope.launch {
            val location = container.locationProvider.currentLocation() ?: return@launch
            _uiState.update {
                it.copy(
                    triggerLatitude = location.latitude,
                    triggerLongitude = location.longitude,
                )
            }
        }
    }

    fun toggleDay(day: DayOfWeek) {
        _uiState.update {
            val days = if (day in it.activeDays) it.activeDays - day else it.activeDays + day
            it.copy(activeDays = days)
        }
    }

    fun setWindowStart(minutes: Int) = _uiState.update { it.copy(windowStartMinutes = minutes) }

    fun setWindowEnd(minutes: Int) = _uiState.update { it.copy(windowEndMinutes = minutes) }

    fun setNotificationsEnabled(enabled: Boolean) =
        _uiState.update { it.copy(notificationsEnabled = enabled) }

    fun setMaxTransfersText(value: String) =
        _uiState.update { it.copy(maxTransfersText = value.filter(Char::isDigit)) }

    fun setMaxTravelMinutesText(value: String) =
        _uiState.update { it.copy(maxTravelMinutesText = value.filter(Char::isDigit)) }

    fun save() {
        val state = _uiState.value
        val origin = state.origin
        val destination = state.destination
        if (origin == null || destination == null || state.name.isBlank()) {
            _uiState.update {
                it.copy(validationError = "Name, start and destination are required")
            }
            return
        }
        if (state.windowEndMinutes <= state.windowStartMinutes) {
            _uiState.update {
                it.copy(validationError = "The time window end must be after its start")
            }
            return
        }

        _uiState.update { it.copy(saving = true, validationError = null) }
        viewModelScope.launch {
            val watcher = Watcher(
                id = editedWatcher?.id ?: 0L,
                name = state.name.trim(),
                origin = origin,
                destination = destination,
                triggerType = state.triggerType,
                triggerLatitude = state.triggerLatitude ?: origin.latitude,
                triggerLongitude = state.triggerLongitude ?: origin.longitude,
                triggerRadiusMeters = state.triggerRadiusMeters,
                activeDays = state.activeDays,
                windowStartMinutes = state.windowStartMinutes,
                windowEndMinutes = state.windowEndMinutes,
                notificationsEnabled = state.notificationsEnabled,
                maxTransfers = state.maxTransfersText.toIntOrNull(),
                maxTravelMinutes = state.maxTravelMinutesText.toIntOrNull(),
                enabled = editedWatcher?.enabled ?: true,
                lastTriggeredAt = editedWatcher?.lastTriggeredAt,
            )
            container.watcherRepository.save(watcher)
            container.triggerSyncRequester.requestSync()
            _uiState.update { it.copy(saving = false, saved = true) }
        }
    }
}
