package com.neddy.ketch.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

/**
 * Fullscreen map picker. Tap to place the marker, use the pinpoint button to
 * jump to the current position, confirm to return the pick. On open the
 * camera zooms to the precise device position unless an initial pick exists.
 * [radiusMeters], when set, previews the leave radius around the pick. The
 * map switches to a dark style when the app theme is dark.
 */
@Composable
fun MapPickerDialog(
    title: String,
    initial: LatLng?,
    radiusMeters: Int?,
    myLocationEnabled: Boolean,
    currentLocation: suspend () -> LatLng?,
    onDismiss: () -> Unit,
    onPick: (LatLng) -> Unit,
) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf(initial) }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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

            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
            ) {
                Text(
                    text = "$title, tap the map to place it",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            SmallFloatingActionButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 44.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 96.dp),
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
            }

            Button(
                onClick = { picked?.let(onPick) },
                enabled = picked != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            ) {
                Text("Use this location")
            }
        }
    }
}

private val PRAGUE = LatLng(50.0755, 14.4378)
