package com.neddy.ketch.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neddy.ketch.appContainer
import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.Watcher
import com.neddy.ketch.ui.components.ConnectionCard
import com.neddy.ketch.ui.components.ConnectionCardSkeleton
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * What a long press on a watcher opens: the connection it would notify about
 * right now, the quicker one worth waiting for when there is one, everything
 * the watcher is set to, and the two actions that belong to the row itself.
 */
@Composable
fun WatcherDetailScreen(
    watcherId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: WatcherDetailViewModel = viewModel {
        WatcherDetailViewModel(context.appContainer, watcherId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted, state.missing) {
        if (state.deleted || state.missing) onBack()
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                watcher = state.watcher,
                refreshing = state.refreshing || state.loading,
                onBack = onBack,
                onRefresh = viewModel::refresh,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val watcher = state.watcher
            val main = state.main

            SectionLabel("Next connection")
            when {
                state.loading -> ConnectionCardSkeleton()
                main != null && watcher != null -> ConnectionCard(
                    title = watcher.name,
                    connection = main,
                    titleIconKey = watcher.icon,
                    subtitle = "To ${watcher.destination.name}",
                )
                else -> NoticeCard(
                    icon = Icons.Filled.EventBusy,
                    title = "No connection found right now",
                    body = state.error
                        ?: "Nothing departs within your limits. Pull the refresh above.",
                )
            }

            val quicker = state.quicker
            if (main != null && quicker != null) {
                SectionLabel("Wait for a quicker one")
                ConnectionCard(
                    title = "Leaves ${formatTime(quicker.departureTime)}",
                    connection = quicker,
                    subtitle = alternativeSubtitle(main, quicker),
                )
            }

            if (watcher != null) {
                SectionLabel("Watcher")
                WatcherFacts(watcher)
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onEdit(watcherId) },
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Edit", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { confirmDelete = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Delete", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Deleting from here has no undo bar behind it, unlike the home list, so it
    // asks first.
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this watcher?") },
            text = { Text("${state.watcher?.name ?: "It"} will stop watching for good.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete()
                    },
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DetailTopBar(
    watcher: Watcher?,
    refreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = watcher?.name.orEmpty(),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onRefresh,
                enabled = !refreshing,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** Everything the watcher is set to, one line per setting. */
@Composable
private fun WatcherFacts(watcher: Watcher) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        FactRow(
            icon = Icons.Filled.Place,
            label = "Destination",
            value = watcher.destination.name,
        )
        FactRow(
            icon = Icons.Filled.CalendarMonth,
            label = "Active",
            value = "${activeDaysText(watcher.activeDays)} · " +
                "${timeFormatter.format(watcher.windowStart)}–" +
                timeFormatter.format(watcher.windowEnd),
        )
        watcher.carStart?.let {
            FactRow(
                icon = Icons.Filled.DirectionsCar,
                label = "Car start",
                value = it.name,
            )
        }
        watcher.preferredVehicle?.let {
            FactRow(
                icon = Icons.Filled.Star,
                label = "Prefers",
                value = it.label + (
                    watcher.maxTravelDeltaMinutes
                        ?.let { delta -> " · up to $delta min slower" }
                        .orEmpty()
                    ),
            )
        }
        FactRow(
            icon = Icons.Filled.Tune,
            label = "Limits",
            value = limitsText(watcher),
        )
    }
}

@Composable
private fun FactRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
    )
}

@Composable
private fun NoticeCard(icon: ImageVector, title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = body,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The one thing the alternative is for: later out the door, shorter journey. */
private fun alternativeSubtitle(main: TransitConnection, quicker: TransitConnection): String {
    val later = Duration.between(main.departureTime, quicker.departureTime).toMinutes()
    val shorter = main.travelDuration.minus(quicker.travelDuration).toMinutes()
    return "$later min later · $shorter min shorter"
}

private fun formatTime(instant: java.time.Instant): String =
    timeFormatter.format(instant.atZone(ZoneId.systemDefault()))

private fun activeDaysText(days: Set<DayOfWeek>): String {
    if (days.isEmpty()) return "Never"
    if (days.size == 7) return "Every day"
    return DayOfWeek.entries
        .filter { it in days }
        .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

private fun limitsText(watcher: Watcher): String {
    val parts = buildList {
        watcher.maxTransfers?.let {
            add(if (it == 0) "direct only" else "max $it transfers")
        }
        watcher.maxTravelMinutes?.let { add("max $it min") }
    }
    return if (parts.isEmpty()) "None" else parts.joinToString(" · ")
}
