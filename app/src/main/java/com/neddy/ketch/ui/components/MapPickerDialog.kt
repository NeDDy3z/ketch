package com.neddy.ketch.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.neddy.ketch.R
import com.neddy.ketch.domain.model.PlaceSuggestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Rounded map picker dialog matching the app theme. Search an address, tap
 * the map, or pinpoint the current position, then confirm. [radiusMeters],
 * when set, previews the leave radius around the pick. The map switches to a
 * dark style when the app theme is dark.
 */
@Composable
fun MapPickerDialog(
    title: String,
    initial: LatLng?,
    radiusMeters: Int?,
    myLocationEnabled: Boolean,
    currentLocation: suspend () -> LatLng?,
    searchPlaces: suspend (String) -> List<PlaceSuggestion>,
    onDismiss: () -> Unit,
    onPick: (LatLng) -> Unit,
) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf(initial) }
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val start = initial ?: PRAGUE
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, if (initial != null) 16f else 11f)
    }
    val markerState = remember { MarkerState(position = start) }
    picked?.let { markerState.position = it }

    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mapStyle = remember(darkTheme) {
        if (darkTheme) {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
        } else {
            null
        }
    }
    val radiusFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val radiusStroke = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        if (initial == null) {
            currentLocation()?.let { location ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(location, 17f),
                )
            }
        }
    }

    LaunchedEffect(query) {
        if (query.length < 3) {
            suggestions = emptyList()
            searching = false
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        suggestions = searchPlaces(query)
        searching = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search address or place") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
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
                suggestions.take(3).forEach { suggestion ->
                    ListItem(
                        headlineContent = { Text(suggestion.name) },
                        supportingContent = {
                            suggestion.address?.let { Text(it) }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val position =
                                    LatLng(suggestion.latitude, suggestion.longitude)
                                picked = position
                                query = suggestion.name
                                suggestions = emptyList()
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(position, 16f),
                                    )
                                }
                            },
                    )
                    HorizontalDivider()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(4.dp)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = myLocationEnabled,
                            mapStyleOptions = mapStyle,
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false,
                        ),
                        onMapClick = { picked = it },
                    ) {
                        picked?.let { center ->
                            Marker(state = markerState)
                            if (radiusMeters != null) {
                                Circle(
                                    center = center,
                                    radius = radiusMeters.toDouble(),
                                    fillColor = radiusFill,
                                    strokeColor = radiusStroke,
                                    strokeWidth = 4f,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                val location = currentLocation() ?: return@launch
                                picked = location
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(location, 17f),
                                )
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "Pinpoint my location",
                        )
                    }
                    Button(
                        onClick = { picked?.let(onPick) },
                        enabled = picked != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Use this location")
                    }
                }
            }
        }
    }
}

private val PRAGUE = LatLng(50.0755, 14.4378)
