package com.neddy.ketch.ui.editor

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
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
    var showMapPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (watcherId == null) "New watcher" else "Edit watcher") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = state.canSave,
                    ) {
                        Text("Save")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Name") },
                placeholder = { Text("Leaving home") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Icon")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(watcherIconCatalog, key = { it.first }) { (key, image) ->
                    FilledIconButton(
                        onClick = { viewModel.setIcon(key) },
                        colors = if (state.icon == key) {
                            IconButtonDefaults.filledIconButtonColors()
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(imageVector = image, contentDescription = key)
                    }
                }
            }

            SectionTitle("Destination")
            DestinationSearchField(
                query = state.destinationQuery,
                selected = state.destination != null,
                results = state.searchResults,
                searching = state.searching,
                error = state.searchError,
                onQueryChange = viewModel::setDestinationQuery,
                onSelect = viewModel::selectDestination,
            )

            SectionTitle("Trigger location")
            Text(
                text = "Ketch fires when you leave this place. The route then starts " +
                    "from wherever you are.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (state.hasTriggerLocation) {
                    "Trigger location: %.5f, %.5f".format(
                        state.triggerLatitude,
                        state.triggerLongitude,
                    )
                } else {
                    "No trigger location set yet"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showMapPicker = true }) {
                    Icon(Icons.Filled.Map, contentDescription = null)
                    Text(
                        text = "Pick on map",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(onClick = viewModel::useCurrentLocationAsTrigger) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null)
                    Text(
                        text = "Current location",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Text(
                text = "Leave radius: ${state.triggerRadiusMeters} m",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = state.triggerRadiusMeters.toFloat(),
                onValueChange = { viewModel.setTriggerRadius(it.toInt()) },
                valueRange = 100f..1000f,
                steps = 8,
            )

            SectionTitle("Active days")
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

            SectionTitle("Time window")
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save watcher")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showMapPicker) {
        MapPickerDialog(
            title = "Trigger location",
            initial = if (state.hasTriggerLocation) {
                LatLng(state.triggerLatitude ?: 0.0, state.triggerLongitude ?: 0.0)
            } else {
                null
            },
            onDismiss = { showMapPicker = false },
            onPick = { latLng ->
                viewModel.setTriggerLocation(latLng.latitude, latLng.longitude)
                showMapPicker = false
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
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
                    Text("Pick a stop from the search results")
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
