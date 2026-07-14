package com.neddy.ketch.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.neddy.ketch.appContainer
import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.ui.components.MapPickerDialog
import com.neddy.ketch.ui.components.SkeletonBox
import com.neddy.ketch.ui.components.watcherIconCatalog
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatcherEditScreen(
    watcherId: Long?,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: WatcherEditViewModel = viewModel {
        WatcherEditViewModel(context.appContainer, watcherId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mapPickerTarget by remember { mutableStateOf<MapPickerTarget?>(null) }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (watcherId == null) "New watcher" else "Edit watcher",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(5) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Name") },
                placeholder = { Text("Leaving home") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(watcherIconCatalog, key = { it.first }) { (key, image) ->
                    FilledIconButton(
                        onClick = { viewModel.setIcon(key) },
                        colors = if (state.icon == key) {
                            IconButtonDefaults.filledIconButtonColors()
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(imageVector = image, contentDescription = key)
                    }
                }
            }

            SectionTitle("Trigger")
            OutlinedTextField(
                value = if (state.hasTriggerLocation) {
                    "%.5f, %.5f".format(state.triggerLatitude, state.triggerLongitude)
                } else {
                    ""
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Trigger location") },
                placeholder = { Text("Pick where you leave from") },
                supportingText = { Text("Fires when you leave this place") },
                trailingIcon = {
                    IconButton(onClick = { mapPickerTarget = MapPickerTarget.TRIGGER }) {
                        Icon(Icons.Filled.Map, contentDescription = "Pick on map")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Leave radius: ${state.triggerRadiusMeters} m",
                style = MaterialTheme.typography.bodyMedium,
            )
            RadiusSlider(
                value = state.triggerRadiusMeters,
                onValueChange = viewModel::setTriggerRadius,
            )

            SectionTitle("Destination")
            DestinationSearchField(
                query = state.destinationQuery,
                selected = state.destination != null,
                results = state.searchResults,
                searching = state.searching,
                error = state.searchError,
                onQueryChange = viewModel::setDestinationQuery,
                onSelect = viewModel::selectDestination,
                onOpenMap = { mapPickerTarget = MapPickerTarget.DESTINATION },
            )

            SectionTitle("When")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in state.activeDays,
                        onClick = { viewModel.toggleDay(day) },
                        label = {
                            Text(day.getDisplayName(TextStyle.NARROW, Locale.getDefault()))
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeField(
                    label = "From",
                    minutes = state.windowStartMinutes,
                    onMinutesChange = viewModel::setWindowStart,
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    label = "To",
                    minutes = state.windowEndMinutes,
                    onMinutesChange = viewModel::setWindowEnd,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionTitle("Limits")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.maxTransfersText,
                    onValueChange = viewModel::setMaxTransfersText,
                    label = { Text("Max transfers") },
                    placeholder = { Text("Any") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.maxTravelMinutesText,
                    onValueChange = viewModel::setMaxTravelMinutesText,
                    label = { Text("Max minutes") },
                    placeholder = { Text("Any") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Notifications", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled,
                )
            }

            state.validationError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("Save watcher")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    mapPickerTarget?.let { target ->
        val hasLocationPermission = context.checkSelfPermission(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val isTrigger = target == MapPickerTarget.TRIGGER
        MapPickerDialog(
            title = if (isTrigger) "Trigger location" else "Destination",
            initial = when {
                isTrigger && state.hasTriggerLocation -> LatLng(
                    state.triggerLatitude ?: 0.0,
                    state.triggerLongitude ?: 0.0,
                )
                !isTrigger && state.destination != null -> LatLng(
                    state.destination?.latitude ?: 0.0,
                    state.destination?.longitude ?: 0.0,
                )
                else -> null
            },
            radiusMeters = if (isTrigger) state.triggerRadiusMeters else null,
            myLocationEnabled = hasLocationPermission,
            currentLocation = {
                viewModel.currentLocation()?.let { (lat, lng) -> LatLng(lat, lng) }
            },
            searchPlaces = viewModel::searchAddresses,
            onDismiss = { mapPickerTarget = null },
            onPick = { latLng ->
                if (isTrigger) {
                    viewModel.setTriggerLocation(latLng.latitude, latLng.longitude)
                } else {
                    viewModel.pickDestinationOnMap(latLng.latitude, latLng.longitude)
                }
                mapPickerTarget = null
            },
        )
    }
}

private enum class MapPickerTarget { TRIGGER, DESTINATION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadiusSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.toInt()) },
        valueRange = 100f..1000f,
        steps = 8,
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                thumbSize = DpSize(4.dp, 28.dp),
            )
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun DestinationSearchField(
    query: String,
    selected: Boolean,
    results: List<StopPlace>,
    searching: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onSelect: (StopPlace) -> Unit,
    onOpenMap: () -> Unit,
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Destination stop") },
            placeholder = { Text("Search stops") },
            singleLine = true,
            supportingText = {
                if (query.isNotBlank() && !selected && results.isEmpty() && !searching) {
                    Text("Pick a stop from the results or the map")
                }
            },
            trailingIcon = {
                if (searching) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(20.dp)
                            .height(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconButton(onClick = onOpenMap) {
                        Icon(Icons.Filled.Map, contentDescription = "Pick on map")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (results.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                results.take(6).forEach { stop ->
                    ListItem(
                        headlineContent = { Text(stop.name) },
                        supportingContent = {
                            Text("%.5f, %.5f".format(stop.latitude, stop.longitude))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(stop) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(
    label: String,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = modifier,
    ) {
        Text("$label: %02d:%02d".format(minutes / 60, minutes % 60))
    }

    if (showDialog) {
        val pickerState = rememberTimePickerState(
            initialHour = minutes / 60,
            initialMinute = minutes % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onMinutesChange(pickerState.hour * 60 + pickerState.minute)
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
