package com.neddy.ketch.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neddy.ketch.appContainer
import com.neddy.ketch.data.settings.EditGesture
import com.neddy.ketch.domain.model.Watcher
import com.neddy.ketch.ui.components.ConnectionCard
import com.neddy.ketch.ui.components.ConnectionCardSkeleton
import com.neddy.ketch.ui.components.WatcherCardHeader
import com.neddy.ketch.ui.components.watcherIcon
import java.time.format.DateTimeFormatter

private enum class HomeMode { NORMAL, REORDER, DELETE }

private val REORDER_ROW_HEIGHT = 68.dp
private val LIST_SPACING = 12.dp

private val windowTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun windowText(watcher: Watcher): String =
    "${windowTimeFormatter.format(watcher.windowStart)}–" +
        windowTimeFormatter.format(watcher.windowEnd)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateWatcher: () -> Unit,
    onEditWatcher: (Long) -> Unit,
    onOpenSettings: () -> Unit,
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

    fun deleteSelected() {
        val toDelete = state.watcherConnections
            .filter { it.watcher.id in selected }
            .map { it.watcher }
        viewModel.delete(toDelete)
        exitMode()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            when (mode) {
                HomeMode.NORMAL -> Unit
                HomeMode.REORDER -> ReorderTopBar(
                    onClose = { exitMode() },
                    onDone = { exitMode() },
                )
                HomeMode.DELETE -> DeleteTopBar(
                    count = selected.size,
                    onClose = { exitMode() },
                    onDelete = { deleteSelected() },
                )
            }
        },
        floatingActionButton = {
            if (mode == HomeMode.NORMAL && state.hasWatchers) {
                FloatingActionButton(
                    onClick = onCreateWatcher,
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add watcher",
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        },
        bottomBar = {
            if (mode == HomeMode.DELETE) {
                DeleteBar(count = selected.size, onDelete = { deleteSelected() })
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
                NormalContent(
                    state = state,
                    onCreateWatcher = onCreateWatcher,
                    onEditWatcher = onEditWatcher,
                    onRefresh = viewModel::refresh,
                    onOpenSettings = onOpenSettings,
                    onReorder = { mode = HomeMode.REORDER },
                    onDelete = { mode = HomeMode.DELETE },
                    onEnableWatcher = { viewModel.setEnabled(it, true) },
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onReorder: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(23.dp),
                )
            }
            ToolsMenu(
                hasWatchers = state.watcherConnections.isNotEmpty(),
                onOpenSettings = onOpenSettings,
                onReorder = onReorder,
                onDelete = onDelete,
            )
        }
        Text(
            text = "Ketch",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp),
        )
        val busy = state.loading || state.watcherConnections.any { it.loading }
        if (busy) {
            Row(
                modifier = Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "Finding connections…",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val count = state.watcherConnections.size
            Text(
                text = if (count == 1) "1 watcher" else "$count watchers",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun ToolsMenu(
    hasWatchers: Boolean,
    onOpenSettings: () -> Unit,
    onReorder: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Settings") },
                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenSettings()
                },
            )
            DropdownMenuItem(
                text = { Text("Reorder") },
                leadingIcon = { Icon(Icons.Filled.DragIndicator, contentDescription = null) },
                enabled = hasWatchers,
                onClick = {
                    expanded = false
                    onReorder()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                enabled = hasWatchers,
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun ReorderTopBar(onClose: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Cancel")
        }
        Text(
            text = "Reorder",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDone) {
            Text(
                text = "Done",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DeleteTopBar(count: Int, onClose: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel")
            }
            Text(
                text = "$count selected",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDelete,
                enabled = count > 0,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
            }
        }
    }
}

@Composable
private fun DeleteBar(count: Int, onDelete: () -> Unit) {
    Box(modifier = Modifier.navigationBarsPadding()) {
        Button(
            onClick = onDelete,
            enabled = count > 0,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
                .height(54.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (count) {
                    0 -> "Delete watchers"
                    1 -> "Delete 1 watcher"
                    else -> "Delete $count watchers"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NormalContent(
    state: HomeUiState,
    onCreateWatcher: () -> Unit,
    onEditWatcher: (Long) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onReorder: () -> Unit,
    onDelete: () -> Unit,
    onEnableWatcher: (Watcher) -> Unit,
) {
    if (!state.loading && !state.hasWatchers) {
        EmptyState(
            state = state,
            onCreateWatcher = onCreateWatcher,
            onRefresh = onRefresh,
            onOpenSettings = onOpenSettings,
            onReorder = onReorder,
            onDelete = onDelete,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(LIST_SPACING),
    ) {
        item {
            HomeHeader(
                state = state,
                onRefresh = onRefresh,
                onOpenSettings = onOpenSettings,
                onReorder = onReorder,
                onDelete = onDelete,
            )
        }

        item { PermissionsSection(onGranted = onRefresh) }

        when {
            state.loading -> items(2) { ConnectionCardSkeleton() }

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
                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = { if (tapToEdit) open() },
                        onLongClick = { if (!tapToEdit) open() },
                    ),
                ) {
                    val connection = item.connection
                    when {
                        item.disabled -> DisabledCard(
                            watcher = item.watcher,
                            onEnable = { onEnableWatcher(item.watcher) },
                        )
                        item.loading -> ConnectionCardSkeleton()
                        connection != null -> ConnectionCard(
                            title = item.watcher.name,
                            connection = connection,
                            titleIcon = watcherIcon(item.watcher.icon),
                            subtitle = "To ${item.watcher.destination.name}",
                        )
                        else -> NoConnectionCard(
                            watcher = item.watcher,
                            error = item.error,
                            onRetry = onRefresh,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    state: HomeUiState,
    onCreateWatcher: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onReorder: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            HomeHeader(
                state = state,
                onRefresh = onRefresh,
                onOpenSettings = onOpenSettings,
                onReorder = onReorder,
                onDelete = onDelete,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            PermissionsSection(
                onGranted = onRefresh,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
            )
            Spacer(modifier = Modifier.height(44.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraLarge,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AltRoute,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(52.dp),
                    )
                }
                Text(
                    text = "No watchers yet",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Add a commute and Ketch tells you the fastest way out " +
                        "the door — and pings you as you leave.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
        Button(
            onClick = onCreateWatcher,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 26.dp)
                .height(56.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(21.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create your first watcher",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
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
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (isDragging) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                shadowElevation = if (isDragging) 8.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(REORDER_ROW_HEIGHT)
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) accumulated else 0f
                        val scale = if (isDragging) 1.03f else 1f
                        scaleX = scale
                        scaleY = scale
                        alpha = if (isDragging) 1f else 0.8f
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .pointerInput(item.watcher.id, stepPx) {
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
                    IconTile(
                        icon = watcherIcon(item.watcher.icon),
                        size = 40.dp,
                        cornerRadius = 13.dp,
                        background = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconSize = 22.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.watcher.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "To ${item.watcher.destination.name}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    IconButton(
                        onClick = { move(index, index - 1) },
                        enabled = index > 0,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                    }
                    IconButton(
                        onClick = { move(index, index + 1) },
                        enabled = index < list.lastIndex,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                    }
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
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (checked) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                contentColor = if (checked) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(item.watcher.id) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (checked) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.RadioButtonUnchecked
                        },
                        contentDescription = if (checked) "Selected" else "Not selected",
                        tint = if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.size(24.dp),
                    )
                    IconTile(
                        icon = watcherIcon(item.watcher.icon),
                        size = 40.dp,
                        cornerRadius = 13.dp,
                        background = if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        iconTint = if (checked) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        iconSize = 22.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.watcher.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "To ${item.watcher.destination.name}",
                            fontSize = 12.sp,
                            color = if (checked) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Small rounded icon tile used by the list rows. */
@Composable
private fun IconTile(
    icon: ImageVector,
    size: Dp,
    cornerRadius: Dp,
    background: Color,
    iconTint: Color,
    iconSize: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(background, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun DisabledCard(watcher: Watcher, onEnable: () -> Unit) {
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .drawBehind {
                    val inset = 0.5.dp.toPx()
                    drawRoundRect(
                        color = outlineVariant,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        cornerRadius = CornerRadius(25.5.dp.toPx()),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                            ),
                        ),
                    )
                }
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(
                    icon = watcherIcon(watcher.icon),
                    size = 44.dp,
                    cornerRadius = 14.dp,
                    background = MaterialTheme.colorScheme.surfaceContainerHighest,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 24.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = watcher.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        text = "Paused",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = false,
                    onCheckedChange = { onEnable() },
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Resting — outside ${windowText(watcher)} window",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoConnectionCard(
    watcher: Watcher,
    error: String?,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 15.dp, end = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            WatcherCardHeader(
                title = watcher.name,
                subtitle = "${windowText(watcher)} window",
                titleIcon = watcherIcon(watcher.icon),
            )
            InfoPanel(
                icon = Icons.Filled.EventBusy,
                title = "No connection found right now",
                body = if (error != null && !error.startsWith("No connection found")) {
                    error
                } else {
                    "Nothing departs within your window and limits. " +
                        "We'll keep checking."
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Try again",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Inline informational panel shared by the info and no-connection cards. */
@Composable
private fun InfoPanel(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

@Composable
private fun InfoCard(title: String, body: String) {
    InfoPanel(
        icon = Icons.Outlined.Info,
        title = title,
        body = body,
    )
}

/**
 * Requests the runtime permissions the app needs. Background location has to
 * be requested separately from foreground location on Android 11 and later.
 */
@Composable
private fun PermissionsSection(onGranted: () -> Unit, modifier: Modifier = Modifier) {
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
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    IconTile(
                        icon = Icons.Filled.MyLocation,
                        size = 40.dp,
                        cornerRadius = 12.dp,
                        background = MaterialTheme.colorScheme.primary,
                        iconTint = MaterialTheme.colorScheme.onPrimary,
                        iconSize = 22.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Finish setup",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = buildAnnotatedString {
                                append("Ketch needs ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("location")
                                }
                                append(" (including ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("background location")
                                }
                                append(" for leave triggers) and ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("notifications")
                                }
                                append(" to work.")
                            },
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (hasLocation && !hasBackground) {
                        TextButton(onClick = {
                            launcher.launch(
                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                            )
                        }) {
                            Text(
                                text = "Allow background location",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    if (!hasLocation || !hasNotifications) {
                        Button(
                            onClick = {
                                val permissions = buildList {
                                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                launcher.launch(permissions.toTypedArray())
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = "Grant basic",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
