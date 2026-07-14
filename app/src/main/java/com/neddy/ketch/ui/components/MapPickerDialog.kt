package com.neddy.ketch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

/**
 * Full screen dialog with a map. Tap to place the marker, use the pinpoint
 * button to jump to the current position, confirm to return the pick. The
 * circle previews the leave radius around the picked point.
 */
@Composable
fun MapPickerDialog(
    title: String,
    initial: LatLng?,
    radiusMeters: Int,
    myLocationEnabled: Boolean,
    currentLocation: suspend () -> LatLng?,
    onDismiss: () -> Unit,
    onPick: (LatLng) -> Unit,
) {
    var picked by remember { mutableStateOf(initial) }
    val scope = rememberCoroutineScope()
    val start = initial ?: PRAGUE
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, if (initial != null) 15f else 11f)
    }
    val markerState = remember { MarkerState(position = start) }
    picked?.let { markerState.position = it }

    val radiusFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val radiusStroke = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                )
                Text(
                    text = "Tap the map to place the trigger location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = myLocationEnabled),
                        uiSettings = MapUiSettings(myLocationButtonEnabled = false),
                        onMapClick = { picked = it },
                    ) {
                        picked?.let { center ->
                            Marker(state = markerState)
                            Circle(
                                center = center,
                                radius = radiusMeters.toDouble(),
                                fillColor = radiusFill,
                                strokeColor = radiusStroke,
                                strokeWidth = 4f,
                            )
                        }
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                val location = currentLocation() ?: return@launch
                                picked = location
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(location, 16f),
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "Pinpoint my location",
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { picked?.let(onPick) },
                        enabled = picked != null,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text("Use this location")
                    }
                }
            }
        }
    }
}

private val PRAGUE = LatLng(50.0755, 14.4378)
