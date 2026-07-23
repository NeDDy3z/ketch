package com.neddy.ketch.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neddy.ketch.appContainer
import com.neddy.ketch.data.settings.EditGesture
import com.neddy.ketch.domain.model.Watcher
import com.neddy.ketch.ui.components.ConnectionCard
import com.neddy.ketch.ui.components.ConnectionCardSkeleton
import com.neddy.ketch.ui.components.watcherIcon

private enum class HomeMode { NORMAL, REORDER, DELETE }

private val REORDER_ROW_HEIGHT = 64.dp
private val LIST_SPACING = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateWatcher: () -> Unit,
    onEditWatcher: (Long) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel { HomeViewModel(context.appContainer) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(HomeMode.NORMAL) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Leaving a special mode always clears its transient selection.
    fun exitMode() {
        mode = HomeMode.NORMAL
        selected = emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (mode) {
                            HomeMode.NORMAL -> "Ketch"
                            HomeMode.REORDER -> "Reorder"
                            HomeMode.DELETE -> "Select to delete"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (mode == HomeMode.NORMAL) {
                        ToolsMenu(
                            enabled = state.watcherConnections.isNotEmpty(),
                            onReorder = { mode = HomeMode.REORDER },
                            onDelete = { mode = HomeMode.DELETE },
                        )
                    } else {
                        IconButton(onClick = { exitMode() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (mode == HomeMode.NORMAL && state.hasWatchers) {
                ExtendedFloatingActionButton(
                    onClick = onCreateWatcher,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New watcher") },
                )
            }
        },
        bottomBar = {
            if (mode == HomeMode.DELETE) {
                DeleteBar(
                    count = selected.size,
                    onDelete = {
                        val toDelete = state.watcherConnections
                            .filter { it.watcher.id in selected }
                            .map { it.watcher }
                        viewModel.delete(toDelete)
                        exitMode()
                    },
                )
            }
        },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)

        when (mode) {
            HomeMode.REORDER -> ReorderList(
                items = state.watcherConnections,
                onCommit = viewModel::reorder,
                modifier = contentModifier,
            )

            HomeMode.DELETE -> DeleteList(
                items = state.watcherConnections,
                selected = selected,
                onToggle = { id ->
                    selected = if (id in selected) selected - id else selected + id
                },
                modifier = contentModifier,
            )

            HomeMode.NORMAL -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = contentModifier,
            ) {
                NormalList(
                    state = state,
                    onCreateWatcher = onCreateWatcher,
                    onEditWatcher = onEditWatcher,
                    onSetEnabled = viewModel::setEnabled,
                    onRefresh = viewModel::refresh,
                )
            }
        }
    }
}

@Composable
private fun ToolsMenu(
    enabled: Boolean,
    onReorder: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Tools")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Reorder") },
                leadingIcon = { Icon(Icons.Filled.DragHandle, contentDescription = null) },
                onClick = {
                    expanded = false
                    onReorder()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun DeleteBar(count: Int, onDelete: () -> Unit) {
    Box(modifier = Modifier.navigationBarsPadding()) {
        Button(
            onClick = onDelete,
            enabled = count > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Text(
                text = if (count > 0) "Delete ($count)" else "Delete",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NormalList(
    state: HomeUiState,
    onCreateWatcher: () -> Unit,
    onEditWatcher: (Long) -> Unit,
    onSetEnabled: (Watcher, Boolean) -> Unit,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(LIST_SPACING),
    ) {
        item { PermissionsSection(onGranted = onRefresh) }

        when {
            state.loading -> items(2) { ConnectionCardSkeleton() }

            !state.hasWatchers -> item { EmptyState(onCreateWatcher = onCreateWatcher) }

            state.missingApiKey -> item {
                InfoCard(
                    title = "API key missing",
                    body = "Add your Google Maps Platform API key in Settings " +
                        "to look up connections.",
                )
            }

            else -> itemsIndexed(
                state.watcherConnections,
                key = { _, item -> item.watcher.id },
            ) { _, item ->
                val open = { onEditWatcher(item.watcher.id) }
                val tapToEdit = state.editGesture == EditGesture.TAP
                val enableSwitch: @Composable () -> Unit = {
                    Switch(
                        checked = item.watcher.enabled,
                        onCheckedChange = { onSetEnabled(item.watcher, it) },
                    )
                }
                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = { if (tapToEdit) open() },
                        onLongClick = { if (!tapToEdit) open() },
                    ),
                ) {
                    val connection = item.connection
                    when {
                        item.disabled -> DisabledCard(
                            name = item.watcher.name,
                            trailingContent = enableSwitch,
                        )
                        item.loading -> ConnectionCardSkeleton()
                        connection != null -> ConnectionCard(
                            title = item.watcher.name,
                            connection = connection,
                            titleIcon = watcherIcon(item.watcher.icon),
                            trailingContent = enableSwitch,
                        )
                        else -> InfoCard(
                            title = item.watcher.name,
                            body = item.error ?: "No connection found",
                            trailingContent = enableSwitch,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderList(
    items: List<WatcherConnection>,
    onCommit: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Key only on the id order, not the whole list, so a connection resolving
    // in the background does not reset an in-progress drag.
    var list by remember(items.map { it.watcher.id }) { mutableStateOf(items) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var accumulated by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val stepPx = with(density) { (REORDER_ROW_HEIGHT + LIST_SPACING).toPx() }

    fun move(from: Int, to: Int) {
        if (to < 0 || to > list.lastIndex || from == to) return
        list = list.toMutableList().also { it.add(to, it.removeAt(from)) }
        onCommit(list.map { it.watcher.id })
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(LIST_SPACING),
    ) {
        itemsIndexed(list, key = { _, item -> item.watcher.id }) { index, item ->
            val isDragging = draggingId == item.watcher.id
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(REORDER_ROW_HEIGHT)
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragging) accumulated else 0f },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = watcherIcon(item.watcher.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.watcher.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { move(index, index - 1) },
                        enabled = index > 0,
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                    }
                    IconButton(
                        onClick = { move(index, index + 1) },
                        enabled = index < list.lastIndex,
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                    }
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = "Drag to reorder",
                        modifier = Modifier.pointerInput(item.watcher.id, stepPx) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = item.watcher.id
                                    accumulated = 0f
                                },
                                onDragEnd = {
                                    onCommit(list.map { it.watcher.id })
                                    draggingId = null
                                    accumulated = 0f
                                },
                                onDragCancel = {
                                    draggingId = null
                                    accumulated = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val id = draggingId
                                    val cur = if (id != null) {
                                        list.indexOfFirst { it.watcher.id == id }
                                    } else {
                                        -1
                                    }
                                    if (cur >= 0) {
                                        accumulated += dragAmount.y
                                        if (accumulated > stepPx / 2 && cur < list.lastIndex) {
                                            list = list.toMutableList()
                                                .also { it.add(cur + 1, it.removeAt(cur)) }
                                            accumulated -= stepPx
                                        } else if (accumulated < -stepPx / 2 && cur > 0) {
                                            list = list.toMutableList()
                                                .also { it.add(cur - 1, it.removeAt(cur)) }
                                            accumulated += stepPx
                                        }
                                    }
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteList(
    items: List<WatcherConnection>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(LIST_SPACING),
    ) {
        itemsIndexed(items, key = { _, item -> item.watcher.id }) { _, item ->
            val checked = item.watcher.id in selected
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(item.watcher.id) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = { onToggle(item.watcher.id) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = watcherIcon(item.watcher.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.watcher.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "To ${item.watcher.destination.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisabledCard(name: String, trailingContent: (@Composable () -> Unit)? = null) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Disabled",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingContent()
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onCreateWatcher: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Train,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No watchers yet",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add your destination, the place you leave from and a time " +
                "window. Ketch tells you which connection to catch as you head out.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateWatcher,
            modifier = Modifier.height(52.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(text = "Create your first watcher", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (trailingContent != null) trailingContent()
            }
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
