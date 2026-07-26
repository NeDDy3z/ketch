package com.neddy.ketch.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.neddy.ketch.maps.TransitDirections
import com.neddy.ketch.ui.components.ConnectionCard
import com.neddy.ketch.ui.components.ConnectionCardSkeleton
import com.neddy.ketch.ui.components.WatcherCardHeader
import com.neddy.ketch.ui.components.watcherIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class HomeMode { NORMAL, REORDER, DELETE }

private val REORDER_ROW_HEIGHT = 68.dp
private val REORDER_ROW_RADIUS = 24.dp

/** Shared height of the reorder and multi-select contextual bars. */
private val CONTEXTUAL_BAR_HEIGHT = 52.dp
private val CONTEXTUAL_BAR_ACTION_SIZE = 40.dp

/** Where the header's blur and wash start giving way to the list beneath. */
private const val HEADER_FADE_START = 0.58f
private const val HEADER_BLUR_RADIUS = 26f
private const val EXPANDED_TITLE_SP = 33f
private const val COLLAPSED_TITLE_SP = 19f

/** How long the delete snackbar keeps undo available. */
private const val UNDO_WINDOW_MS = 5_000L
private val LIST_SPACING = 12.dp

private val TabularNumbers = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")

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
    onOpenHelp: () -> Unit,
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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun deleteSelected() {
        val toDelete = state.watcherConnections
            .filter { it.watcher.id in selected }
            .map { it.watcher }
        if (toDelete.isEmpty()) return
        viewModel.delete(toDelete)
        exitMode()
        scope.launch {
            // Held open for exactly the five seconds the delete bar promises,
            // rather than the built-in durations either side of it.
            val timeout = launch {
                delay(UNDO_WINDOW_MS)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
                message = if (toDelete.size == 1) {
                    "1 watcher deleted"
                } else {
                    "${toDelete.size} watchers deleted"
                },
                actionLabel = "Undo",
                duration = SnackbarDuration.Indefinite,
            )
            timeout.cancel()
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            when (mode) {
                // Normal mode has no top bar: the header floats over the list so
                // the cards stay continuous as they pass under it.
                HomeMode.NORMAL -> Unit
                HomeMode.REORDER -> ReorderTopBar(
                    onClose = { exitMode() },
                    onDone = { exitMode() },
                )
                HomeMode.DELETE -> DeleteTopBar(
                    count = selected.size,
                    onClose = { exitMode() },
                    onSelectAll = {
                        val all = state.watcherConnections.map { it.watcher.id }.toSet()
                        selected = if (selected == all) emptySet() else all
                    },
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
            when (mode) {
                HomeMode.DELETE -> DeleteBar(
                    count = selected.size,
                    onDelete = { deleteSelected() },
                )
                HomeMode.REORDER -> ReorderHint()
                HomeMode.NORMAL -> Unit
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

            HomeMode.NORMAL -> {
                val listState = rememberLazyListState()
                // The blurred strip behind the header is a recorded copy of the
                // list, so the two have to share an origin: both fill this Box.
                val backdrop = rememberGraphicsLayer()
                var headerHeight by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current
                Box(modifier = contentModifier) {
                    PullToRefreshBox(
                        isRefreshing = state.refreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        NormalContent(
                            state = state,
                            listState = listState,
                            topInset = headerHeight,
                            backdrop = backdrop,
                            onCreateWatcher = onCreateWatcher,
                            onEditWatcher = onEditWatcher,
                            onRefresh = viewModel::refresh,
                            onEnableWatcher = { viewModel.setEnabled(it, true) },
                        )
                    }
                    HomeHeader(
                        state = state,
                        listState = listState,
                        backdrop = backdrop,
                        onRefresh = viewModel::refresh,
                        onRefreshAll = viewModel::refreshAll,
                        onOpenSettings = onOpenSettings,
                        onOpenHelp = onOpenHelp,
                        onReorder = { mode = HomeMode.REORDER },
                        onDelete = { mode = HomeMode.DELETE },
                        onToggleShowResting = { viewModel.setShowResting(!state.showResting) },
                        modifier = Modifier.onSizeChanged {
                            headerHeight = with(density) { it.height.toDp() }
                        },
                    )
                }
            }
        }
    }
}

/**
 * The header floats over the list rather than sitting in a bar: a blurred copy
 * of the cards passing underneath, faded out towards the bottom so there is no
 * hard edge and the list reads as continuous. The large title shrinks once the
 * list has moved.
 */
@Composable
private fun HomeHeader(
    state: HomeUiState,
    listState: LazyListState,
    backdrop: GraphicsLayer,
    onRefresh: () -> Unit,
    onRefreshAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onReorder: () -> Unit,
    onDelete: () -> Unit,
    onToggleShowResting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 8
        }
    }
    val titleSize by animateFloatAsState(
        targetValue = if (scrolled) COLLAPSED_TITLE_SP else EXPANDED_TITLE_SP,
        label = "homeTitle",
    )
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier = modifier.fillMaxWidth()) {
        // Blur is a RenderEffect, so it only exists from API 31; below that the
        // gradient alone carries the effect.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    // The blur applies to this Box's own output — a copy of the
                    // recorded list — so the list underneath stays sharp.
                    // Offscreen so the fade below can erase the blur itself,
                    // rather than only drawing over it.
                    .graphicsLayer {
                        renderEffect = BlurEffect(
                            HEADER_BLUR_RADIUS,
                            HEADER_BLUR_RADIUS,
                            TileMode.Decal,
                        )
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawLayer(backdrop)
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black,
                                HEADER_FADE_START to Color.Black,
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }
        // Tonal wash over the blur: solid at the status bar, gone by the bottom
        // edge, so cards emerge rather than appearing from behind a band.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to surface,
                        HEADER_FADE_START to surface.copy(alpha = 0.82f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 26.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ketch",
                    fontSize = titleSize.sp,
                    lineHeight = (titleSize * 1.1f).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
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
                    showResting = state.showResting,
                    onRefreshAll = onRefreshAll,
                    onOpenSettings = onOpenSettings,
                    onOpenHelp = onOpenHelp,
                    onReorder = onReorder,
                    onDelete = onDelete,
                    onToggleShowResting = onToggleShowResting,
                )
            }
            // Only the busy line earns a second row; the watcher count did not.
            if (state.loading || state.watcherConnections.any { it.loading }) {
                Row(
                    modifier = Modifier.padding(top = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "Finding connections\u2026",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Overflow, not a settings trip. The four list-level actions sit above a
 * divider, with Settings and Help below. "Refresh all" is the only way to poll
 * resting watchers — the header icon and pull-to-refresh cover the active ones —
 * so the expensive action is one deliberate step away.
 */
@Composable
private fun ToolsMenu(
    hasWatchers: Boolean,
    showResting: Boolean,
    onRefreshAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onReorder: () -> Unit,
    onDelete: () -> Unit,
    onToggleShowResting: () -> Unit,
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
                text = { Text("Refresh all") },
                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                enabled = hasWatchers,
                onClick = {
                    expanded = false
                    onRefreshAll()
                },
            )
            DropdownMenuItem(
                text = { Text("Reorder") },
                leadingIcon = { Icon(Icons.Filled.SwapVert, contentDescription = null) },
                enabled = hasWatchers,
                onClick = {
                    expanded = false
                    onReorder()
                },
            )
            // Toggles inline with the menu staying open, so the list change is
            // visible behind it.
            DropdownMenuItem(
                text = { Text(if (showResting) "Hide resting" else "Show resting") },
                leadingIcon = {
                    Icon(
                        imageVector = if (showResting) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = null,
                    )
                },
                enabled = hasWatchers,
                onClick = onToggleShowResting,
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
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            DropdownMenuItem(
                text = { Text("Settings") },
                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenSettings()
                },
            )
            DropdownMenuItem(
                text = { Text("Help & feedback") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onOpenHelp()
                },
            )
        }
    }
}

/**
 * Contextual bars for reorder and multi-select ride as a floating pill on
 * surface-container-high rather than an edge-to-edge band, so entering a mode
 * reads as one surface morphing instead of a navigation push.
 */
@Composable
private fun ContextualBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 4.dp,
            // Fixed so reorder and multi-select are the same height whatever
            // their actions are; a Button would otherwise claim a taller
            // minimum touch target than an icon and grow the bar.
            modifier = Modifier
                .fillMaxWidth()
                .height(CONTEXTUAL_BAR_HEIGHT),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** The circular close affordance that exits a contextual mode. */
@Composable
private fun BarCloseButton(onClose: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(CONTEXTUAL_BAR_ACTION_SIZE),
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Cancel",
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun ReorderTopBar(onClose: () -> Unit, onDone: () -> Unit) {
    ContextualBar {
        BarCloseButton(onClose = onClose)
        Text(
            text = "Reorder",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp),
        )
        Button(
            onClick = onDone,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
            modifier = Modifier.height(CONTEXTUAL_BAR_ACTION_SIZE),
        ) {
            Text(
                text = "Done",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DeleteTopBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
) {
    ContextualBar {
        BarCloseButton(onClose = onClose)
        Text(
            text = "$count selected",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            style = TabularNumbers,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
        IconButton(onClick = onSelectAll, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Filled.SelectAll,
                contentDescription = "Select all",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
        }
        // The only place error appears at full strength.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(40.dp),
        ) {
            IconButton(onClick = onDelete, enabled = count > 0) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete selected",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun DeleteBar(count: Int, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onDelete,
            enabled = count > 0,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(9.dp))
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
        // Deleting acts immediately and is undoable, rather than asking first.
        Text(
            text = "Undo stays available for 5 s",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NormalContent(
    state: HomeUiState,
    listState: LazyListState,
    topInset: Dp,
    backdrop: GraphicsLayer,
    onCreateWatcher: () -> Unit,
    onEditWatcher: (Long) -> Unit,
    onRefresh: () -> Unit,
    onEnableWatcher: (Watcher) -> Unit,
) {
    if (!state.loading && !state.hasWatchers) {
        EmptyState(
            onCreateWatcher = onCreateWatcher,
            onRefresh = onRefresh,
        )
        return
    }

    val context = LocalContext.current

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            // Recorded so the header can draw a blurred copy of whatever is
            // scrolling under it, then drawn normally.
            .drawWithContent {
                backdrop.record { this@drawWithContent.drawContent() }
                drawLayer(backdrop)
            },
        contentPadding = PaddingValues(
            start = 16.dp,
            top = topInset,
            end = 16.dp,
            bottom = 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(LIST_SPACING),
    ) {
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

            state.allRestingHidden -> item {
                InfoCard(
                    title = "Every watcher is resting",
                    body = "Nothing is inside its active window right now. " +
                        "Turn on “Show resting” in the menu to see them anyway.",
                )
            }

            else -> itemsIndexed(
                state.visibleWatcherConnections,
                key = { _, item -> item.watcher.id },
            ) { _, item ->
                val open = { onEditWatcher(item.watcher.id) }
                val tapToEdit = state.editGesture == EditGesture.TAP
                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = { if (tapToEdit) open() },
                        onLongClick = { if (!tapToEdit) open() },
                        // Double tap hands the route to Google Maps as public
                        // transport directions to the watcher destination, when
                        // the gesture is enabled in Settings.
                        onDoubleClick = if (state.doubleTapOpensMaps) {
                            { TransitDirections.open(context, item.watcher.destination) }
                        } else {
                            null
                        },
                    ),
                ) {
                    val connection = item.connection
                    // Off, not broken: a resting watcher stays scannable but goes
                    // visually quiet rather than looking like it crashed.
                    val restingAlpha = if (item.resting && !item.disabled) 0.7f else 1f
                    Box(modifier = Modifier.graphicsLayer { alpha = restingAlpha }) {
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
}

@Composable
private fun EmptyState(
    onCreateWatcher: () -> Unit,
    onRefresh: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            PermissionsSection(
                onGranted = onRefresh,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp),
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
            val outlineVariant = MaterialTheme.colorScheme.outlineVariant
            val dropHint = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(REORDER_ROW_HEIGHT)
                    .zIndex(if (isDragging) 1f else 0f),
            ) {
                // The slot the card will drop into, shown as a dashed outline
                // while the lifted card floats away from it.
                if (isDragging) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                val inset = 1.dp.toPx()
                                drawRoundRect(
                                    color = dropHint,
                                    cornerRadius = CornerRadius(REORDER_ROW_RADIUS.toPx()),
                                )
                                drawRoundRect(
                                    color = outlineVariant,
                                    topLeft = Offset(inset, inset),
                                    size = Size(
                                        size.width - inset * 2,
                                        size.height - inset * 2,
                                    ),
                                    cornerRadius = CornerRadius(REORDER_ROW_RADIUS.toPx()),
                                    style = Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                                        ),
                                    ),
                                )
                            },
                    )
                }
                Surface(
                    shape = RoundedCornerShape(REORDER_ROW_RADIUS),
                    color = if (isDragging) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    shadowElevation = if (isDragging) 16.dp else 0.dp,
                    border = if (isDragging) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = if (isDragging) accumulated else 0f
                            val scale = if (isDragging) 1.03f else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                        // Long-press anywhere on the row to lift it; the handle
                        // is an affordance, not the only target.
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
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = null,
                        tint = if (isDragging) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.size(22.dp),
                    )
                    IconTile(
                        icon = watcherIcon(item.watcher.icon),
                        size = 36.dp,
                        cornerRadius = 12.dp,
                        background = if (isDragging) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        iconTint = if (isDragging) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        iconSize = 20.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.watcher.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (isDragging) {
                                "Dragging · position ${index + 1} of ${list.size}"
                            } else {
                                rowSubtitle(item)
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                }
            }
        }
    }
}

/** The hint that reorder saves itself, pinned above the navigation bar. */
@Composable
private fun ReorderHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.SwipeVertical,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text(text = "Drag to reorder · saves on drop", fontSize = 12.sp)
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
                shape = RoundedCornerShape(REORDER_ROW_RADIUS),
                // Selection rides a secondary-container tint, not primary, so it
                // never competes with the duration pill's voice.
                color = if (checked) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                contentColor = if (checked) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                border = if (checked) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(item.watcher.id) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelectionBox(checked = checked)
                    IconTile(
                        icon = watcherIcon(item.watcher.icon),
                        size = 38.dp,
                        cornerRadius = 12.dp,
                        background = if (checked) {
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        iconTint = if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        iconSize = 21.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.watcher.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = rowSubtitle(item),
                            fontSize = 12.sp,
                            color = if (checked) {
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** The square checkbox on a multi-select row. */
@Composable
private fun SelectionBox(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .then(
                if (checked) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(7.dp),
                    )
                } else {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(7.dp),
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * The one-line summary a collapsed row carries: duration and the journey's two
 * ends once a connection has resolved, the destination until then.
 */
private fun rowSubtitle(item: WatcherConnection): String {
    val connection = item.connection ?: return "To ${item.watcher.destination.name}"
    val zone = ZoneId.systemDefault()
    val departure = windowTimeFormatter.format(
        connection.legs.first().departureTime.atZone(zone),
    )
    val arrival = windowTimeFormatter.format(connection.legs.last().arrivalTime.atZone(zone))
    return "${connection.travelDuration.toMinutes()} min · $departure → $arrival"
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
