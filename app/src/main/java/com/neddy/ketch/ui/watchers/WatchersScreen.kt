package com.neddy.ketch.ui.watchers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.neddy.ketch.appContainer
import com.neddy.ketch.domain.model.TriggerType
import com.neddy.ketch.domain.model.Watcher
import com.neddy.ketch.ui.components.SkeletonBox
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchersScreen(
    onAddWatcher: () -> Unit,
    onEditWatcher: (Long) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: WatchersViewModel = viewModel {
        WatchersViewModel(context.appContainer)
    }
    val watchers by viewModel.watchers.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Watcher?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Watchers") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWatcher) {
                Icon(Icons.Filled.Add, contentDescription = "Add watcher")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val list = watchers
            when {
                list == null -> {
                    items(3) {
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp),
                        )
                    }
                }

                list.isEmpty() -> {
                    item {
                        Text(
                            text = "No watchers yet. Tap the add button to create one.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }

                else -> {
                    items(list, key = { it.id }) { watcher ->
                        WatcherCard(
                            watcher = watcher,
                            onClick = { onEditWatcher(watcher.id) },
                            onToggle = { viewModel.setEnabled(watcher, it) },
                            onDelete = { pendingDelete = watcher },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { watcher ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete watcher") },
            text = { Text("Delete \"${watcher.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(watcher)
                    pendingDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun WatcherCard(
    watcher: Watcher,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = watcher.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = watcher.enabled,
                    onCheckedChange = onToggle,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                text = "${watcher.origin.name} to ${watcher.destination.name}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (watcher.triggerType == TriggerType.LOCATION_EXIT) {
                        Icons.Filled.Place
                    } else {
                        Icons.Filled.Schedule
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = watcherSubtitle(watcher),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun watcherSubtitle(watcher: Watcher): String {
    val days = watcher.activeDays
        .sortedBy { it.value }
        .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    val window = "%02d:%02d to %02d:%02d".format(
        watcher.windowStartMinutes / 60,
        watcher.windowStartMinutes % 60,
        watcher.windowEndMinutes / 60,
        watcher.windowEndMinutes % 60,
    )
    val trigger = if (watcher.triggerType == TriggerType.LOCATION_EXIT) {
        "On leave"
    } else {
        "At time"
    }
    return "$trigger, $days, $window"
}
