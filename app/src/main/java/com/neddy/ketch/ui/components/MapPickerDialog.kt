package com.neddy.ketch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Full-bleed map picker dialog. The map fills the screen behind a floating
 * search pill, a my-location button, and a bottom confirm sheet. Search an
 * address, tap the map, or pinpoint the current position, then confirm.
 * [radiusMeters], when set, previews the leave radius around the pick. The
 * map switches to a dark style when the app theme is dark. [isTrigger]
 * selects the sheet icon (trigger origin vs. destination place); it defaults
 * to whether a radius applies so existing call sites keep working.
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
    isTrigger: Boolean = radiusMeters != null,
) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf(initial) }
    var pickedLabel by remember { mutableStateOf<String?>(null) }
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
    val floatingColor = if (darkTheme) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surface
    }
    val sheetColor = if (darkTheme) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surface
    }
    val radiusFill = MaterialTheme.colorScheme.primary.copy(
        alpha = if (darkTheme) 0.20f else 0.16f,
    )
    val radiusStroke = MaterialTheme.colorScheme.primary
    val radiusStrokeWidth = with(LocalDensity.current) { 2.dp.toPx() }

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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
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
                contentPadding = PaddingValues(top = 88.dp, bottom = 152.dp),
                onMapClick = {
                    picked = it
                    pickedLabel = null
                },
            ) {
                picked?.let { center ->
                    Marker(state = markerState)
                    if (radiusMeters != null) {
                        Circle(
                            center = center,
                            radius = radiusMeters.toDouble(),
                            fillColor = radiusFill,
                            strokeColor = radiusStroke,
                            strokeWidth = radiusStrokeWidth,
                        )
                    }
                }
            }

            // Floating search pill + suggestions.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
            ) {
                Surface(
                    color = floatingColor,
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 18.dp, end = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(onClick = onDismiss),
                        )
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "Search address or place",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 15.sp,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                ),
                        ) {
                            if (searching) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = floatingColor,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            suggestions.take(3).forEachIndexed { index, suggestion ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val position = LatLng(
                                                suggestion.latitude,
                                                suggestion.longitude,
                                            )
                                            picked = position
                                            pickedLabel = suggestion.name
                                            query = suggestion.name
                                            suggestions = emptyList()
                                            scope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(
                                                        position,
                                                        16f,
                                                    ),
                                                )
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Column {
                                        Text(
                                            text = suggestion.name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text = suggestion.address ?: String.format(
                                                "%.5f, %.5f",
                                                suggestion.latitude,
                                                suggestion.longitude,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating layers above the bottom sheet + the sheet itself.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                ) {
                    if (radiusMeters != null && picked != null) {
                        Surface(
                            color = floatingColor,
                            shape = CircleShape,
                            shadowElevation = 2.dp,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(
                                    horizontal = 11.dp,
                                    vertical = 4.dp,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Radar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = "$radiusMeters m",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    Surface(
                        color = floatingColor,
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 6.dp,
                        onClick = {
                            scope.launch {
                                val location = currentLocation() ?: return@launch
                                picked = location
                                pickedLabel = null
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(location, 17f),
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 18.dp)
                            .size(56.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = "Pinpoint my location",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }

                Surface(
                    color = sheetColor,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 26.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 16.dp)
                                .width(32.dp)
                                .height(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(2.dp),
                                ),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(14.dp),
                                    ),
                            ) {
                                Icon(
                                    imageVector = if (isTrigger) {
                                        Icons.Filled.TripOrigin
                                    } else {
                                        Icons.Filled.Place
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(23.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                val place = pickedLabel ?: picked?.let {
                                    String.format("%.5f, %.5f", it.latitude, it.longitude)
                                } ?: "Tap the map to choose a point"
                                val radiusSuffix =
                                    if (radiusMeters != null && picked != null) {
                                        " · leave radius $radiusMeters m"
                                    } else {
                                        ""
                                    }
                                Text(
                                    text = place + radiusSuffix,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                        Button(
                            onClick = { picked?.let(onPick) },
                            enabled = picked != null,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(21.dp),
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                            Text(
                                text = "Use this location",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val PRAGUE = LatLng(50.0755, 14.4378)
