package com.neddy.ketch.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neddy.ketch.appContainer
import com.neddy.ketch.ui.components.ConnectionCard
import com.neddy.ketch.ui.components.ConnectionCardSkeleton
import com.neddy.ketch.ui.components.watcherIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateWatcher: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel { HomeViewModel(context.appContainer) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ketch", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PermissionsSection(onGranted = { viewModel.refresh() }) }

            when {
                state.loading -> {
                    items(2) { ConnectionCardSkeleton() }
                }

                !state.hasWatchers -> {
                    item {
                        EmptyState(onCreateWatcher = onCreateWatcher)
                    }
                }

                state.missingApiKey -> {
                    item {
                        InfoCard(
                            title = "API key missing",
                            body = "Add your Google Maps Platform API key in Settings " +
                                "to look up connections.",
                        )
                    }
                }

                else -> {
                    items(state.watcherConnections.size) { index ->
                        val item = state.watcherConnections[index]
                        val connection = item.connection
                        if (item.loading) {
                            ConnectionCardSkeleton()
                        } else if (connection != null) {
                            ConnectionCard(
                                title = item.watcher.name,
                                connection = connection,
                                titleIcon = watcherIcon(item.watcher.icon),
                            )
                        } else {
                            InfoCard(
                                title = item.watcher.name,
                                body = item.error ?: "No connection found",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onCreateWatcher: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "No watchers yet",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create a watcher with your destination, trigger location " +
                    "and time window. Ketch will tell you which connection to catch " +
                    "when you leave.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCreateWatcher) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(text = "Create watcher", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Requests the runtime permissions the app needs. Background location has to
 * be requested separately from foreground location on Android 11 and later.
 */
@Composable
private fun PermissionsSection(onGranted: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }

    val hasNotifications = remember(refreshKey) {
        Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(
            Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val hasLocation = remember(refreshKey) {
        context.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val hasBackground = remember(refreshKey) {
        context.checkSelfPermission(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshKey++
        onGranted()
    }

    if (!hasNotifications || !hasLocation || !hasBackground) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Permissions needed",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ketch needs location (including background access for " +
                        "leave triggers) and notifications to work.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasLocation || !hasNotifications) {
                        Button(onClick = {
                            val permissions = buildList {
                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (Build.VERSION.SDK_INT >= 33) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            launcher.launch(permissions.toTypedArray())
                        }) {
                            Text("Grant basic")
                        }
                    }
                    if (hasLocation && !hasBackground) {
                        Button(onClick = {
                            launcher.launch(
                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                            )
                        }) {
                            Text("Allow background location")
                        }
                    }
                }
            }
        }
    }
}
