package com.neddy.ketch.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.neddy.ketch.appContainer
import com.neddy.ketch.domain.model.PlaceSuggestion
import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.VehicleCategory
import com.neddy.ketch.ui.components.MapPickerDialog
import com.neddy.ketch.ui.components.SkeletonBox
import com.neddy.ketch.ui.components.watcherIconCatalog
import java.time.DayOfWeek
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
            EditorTopBar(
                title = if (watcherId == null) "New watcher" else "Edit watcher",
                canSave = state.canSave,
                onBack = onDone,
                onSave = viewModel::save,
            )
        },
    ) { padding ->
        if (state.loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(top = 6.dp, start = 18.dp, end = 18.dp, bottom = 26.dp),
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
                .padding(top = 6.dp, start = 18.dp, end = 18.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // Icon picker
            Column {
                Text(
                    text = "ICON",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    contentPadding = PaddingValues(bottom = 6.dp),
                ) {
                    items(watcherIconCatalog, key = { it.first }) { (key, image) ->
                        val selected = state.icon == key
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                )
                                .clickable { viewModel.setIcon(key) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = image,
                                contentDescription = key,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(23.dp),
                            )
                        }
                    }
                }
            }

            NameField(
                value = state.name,
                onValueChange = viewModel::setName,
            )

            // Trigger
            EditorSection(
                icon = Icons.Filled.TripOrigin,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Trigger",
            ) {
                SearchField(
                    query = state.triggerQuery,
                    placeholder = "Search an address, station or stop",
                    searching = state.triggerSearching,
                    isError = false,
                    showCheck = false,
                    onQueryChange = viewModel::setTriggerQuery,
                    onOpenMap = { mapPickerTarget = MapPickerTarget.TRIGGER },
                )
                state.triggerSearchError?.let { InlineErrorHelper(it) }
                if (state.triggerResults.isNotEmpty()) {
                    SuggestionList(
                        items = state.triggerResults.take(6),
                        title = { it.name },
                        subtitle = { it.address },
                        onSelect = viewModel::selectTriggerSuggestion,
                    )
                }
                LeaveRadiusCard(
                    value = state.triggerRadiusMeters,
                    onValueChange = viewModel::setTriggerRadius,
                )
            }

            // Destination
            EditorSection(
                icon = Icons.Filled.Place,
                iconTint = MaterialTheme.colorScheme.tertiary,
                title = "Destination",
            ) {
                val destinationMissing = state.destination == null
                SearchField(
                    query = state.destinationQuery,
                    placeholder = "Search a stop",
                    searching = state.searching,
                    isError = destinationMissing,
                    showCheck = !destinationMissing,
                    onQueryChange = viewModel::setDestinationQuery,
                    onOpenMap = { mapPickerTarget = MapPickerTarget.DESTINATION },
                )
                if (destinationMissing) {
                    InlineErrorHelper("Choose a destination stop to save this watcher")
                }
                state.searchError?.let { InlineErrorHelper(it) }
                if (state.searchResults.isNotEmpty()) {
                    SuggestionList(
                        items = state.searchResults.take(6),
                        title = { it.name },
                        subtitle = { "%.5f, %.5f".format(it.latitude, it.longitude) },
                        onSelect = viewModel::selectDestination,
                    )
                }
            }

            // When
            EditorSection(
                icon = Icons.Filled.CalendarMonth,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "When",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val selected = day in state.activeDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                )
                                .clickable { viewModel.toggleDay(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = day.getDisplayName(
                                    java.time.format.TextStyle.NARROW,
                                    Locale.getDefault(),
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
                if (state.activeDays.isEmpty()) {
                    InlineErrorHelper("Pick at least one active day")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TimeCard(
                        label = "From",
                        minutes = state.windowStartMinutes,
                        onMinutesChange = viewModel::setWindowStart,
                        modifier = Modifier.weight(1f),
                    )
                    TimeCard(
                        label = "To",
                        minutes = state.windowEndMinutes,
                        onMinutesChange = viewModel::setWindowEnd,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Limits
            EditorSection(
                icon = Icons.Filled.Tune,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Limits",
                titleSuffix = " · optional",
            ) {
                MaxTransfersStepper(
                    valueText = state.maxTransfersText,
                    onValueTextChange = viewModel::setMaxTransfersText,
                )
                OverlineValueField(
                    overline = "Max travel minutes",
                    value = state.maxTravelMinutesText,
                    placeholder = "Any",
                    suffix = "min",
                    onValueChange = viewModel::setMaxTravelMinutesText,
                )
            }

            // Preferred connection
            EditorSection(
                icon = Icons.Filled.Star,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Preferred connection",
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VehicleCategory.selectable.forEach { category ->
                        val selected = state.preferredVehicle == category
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setPreferredVehicle(category) },
                            shape = CircleShape,
                            label = {
                                Text(
                                    text = category.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Medium
                                    },
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (selected) {
                                        Icons.Filled.Check
                                    } else {
                                        vehicleIcon(category)
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor =
                                    MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor =
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedLeadingIconColor =
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = MaterialTheme.colorScheme.outline,
                                borderWidth = 1.dp,
                            ),
                        )
                    }
                }
                if (state.preferredVehicle != null) {
                    OverlineValueField(
                        overline = "Tolerance vs. fastest",
                        value = state.maxTravelDeltaMinutesText,
                        placeholder = "Always prefer",
                        suffix = "min",
                        onValueChange = viewModel::setMaxTravelDeltaMinutesText,
                    )
                }
            }

            // Toggles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                ToggleRow(
                    icon = Icons.Filled.ToggleOn,
                    label = "Enabled",
                    checked = state.enabled,
                    onCheckedChange = viewModel::setEnabled,
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                ToggleRow(
                    icon = Icons.Filled.Notifications,
                    label = "Notify me as I leave",
                    checked = state.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled,
                )
            }

            state.validationError?.let { InlineErrorHelper(it) }
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

@Composable
private fun EditorTopBar(
    title: String,
    canSave: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 14.dp, end = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (canSave) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    )
                    .clickable(enabled = canSave, onClick = onSave)
                    .padding(vertical = 9.dp, horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canSave) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "Save",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canSave) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun EditorSection(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    titleSuffix: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (titleSuffix != null) {
                    Text(
                        text = titleSuffix,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        content()
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(top = 9.dp, start = 16.dp, end = 16.dp, bottom = 11.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Name",
            fontSize = 12.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (focused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Box {
            if (value.isEmpty()) {
                Text(
                    text = "Leaving home",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = interactionSource,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    if (focused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ),
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    placeholder: String,
    searching: Boolean,
    isError: Boolean,
    showCheck: Boolean,
    onQueryChange: (String) -> Unit,
    onOpenMap: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (isError) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.error, shape)
                } else {
                    Modifier
                },
            )
            .padding(start = 16.dp, end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(21.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (showCheck && !searching) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (searching) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(21.dp),
                    strokeWidth = 2.dp,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onOpenMap),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = "Pick on map",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@Composable
private fun <T> SuggestionList(
    items: List<T>,
    title: (T) -> String,
    subtitle: (T) -> String?,
    onSelect: (T) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item) }
                    .padding(vertical = 11.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = title(item),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    subtitle(item)?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveRadiusCard(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Leave radius",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$value m",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "100 m",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "1000 m",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InlineErrorHelper(text: String) {
    Row(
        modifier = Modifier.padding(start = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeCard(
    label: String,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { showDialog = true }
            .padding(top = 9.dp, start = 16.dp, end = 16.dp, bottom = 11.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "%02d:%02d".format(minutes / 60, minutes % 60),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
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

@Composable
private fun MaxTransfersStepper(
    valueText: String,
    onValueTextChange: (String) -> Unit,
) {
    val current = valueText.toIntOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 13.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Max transfers",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Decrease max transfers",
                enabled = current != null,
                onClick = {
                    when {
                        current == null -> Unit
                        current <= 0 -> onValueTextChange("")
                        else -> onValueTextChange((current - 1).toString())
                    }
                },
            )
            Text(
                text = current?.toString() ?: "Any",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                style = TextStyle(fontFeatureSettings = "tnum"),
                modifier = Modifier.widthIn(min = 14.dp),
            )
            StepperButton(
                icon = Icons.Filled.Add,
                contentDescription = "Increase max transfers",
                enabled = true,
                onClick = {
                    onValueTextChange(((current ?: -1) + 1).toString())
                },
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun OverlineValueField(
    overline: String,
    value: String,
    placeholder: String,
    suffix: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(top = 9.dp, start = 16.dp, end = 16.dp, bottom = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = overline,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFeatureSettings = "tnum",
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Text(
            text = suffix,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun vehicleIcon(category: VehicleCategory): ImageVector = when (category) {
    VehicleCategory.TRAIN -> Icons.Filled.DirectionsRailway
    VehicleCategory.BUS -> Icons.Filled.DirectionsBus
    VehicleCategory.TRAM -> Icons.Filled.Tram
    VehicleCategory.SUBWAY -> Icons.Filled.DirectionsSubway
    VehicleCategory.FERRY -> Icons.Filled.DirectionsBoat
    VehicleCategory.OTHER -> Icons.Filled.DirectionsBus
}
