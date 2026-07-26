package com.neddy.ketch.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neddy.ketch.appContainer
import com.neddy.ketch.data.settings.ColorPalette
import com.neddy.ketch.domain.model.Watcher
import com.neddy.ketch.ui.components.watcherIcon
import com.neddy.ketch.ui.theme.KetchTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Configuration shown when the widget is dropped on the home screen, and
 * again whenever the widget's logo tile is tapped. The user picks which
 * watchers the widget pages through.
 */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val container = appContainer
        val paletteFlow = container.settingsRepository.settings.map { it.palette }
        val watchersFlow = container.watcherRepository.observeWatchers()
        val initialSelection = WidgetPrefs.selectedWatchers(this, appWidgetId).toSet()
        val initialOnlyActive = WidgetPrefs.showOnlyActive(this, appWidgetId)
        val initialTheme = WidgetPrefs.theme(this, appWidgetId)

        setContent {
            val palette by paletteFlow.collectAsStateWithLifecycle(ColorPalette.DEFAULT)
            KetchTheme(palette = palette) {
                ConfigContent(
                    watchersFlow = watchersFlow,
                    initialSelection = initialSelection,
                    initialOnlyActive = initialOnlyActive,
                    initialTheme = initialTheme,
                    // An existing selection means the widget is already on the
                    // home screen and is only being reconfigured.
                    confirmLabel = if (initialSelection.isEmpty()) "Add widget" else "Save",
                    onBack = { finish() },
                    onConfirm = { selected, onlyActive, theme ->
                        WidgetPrefs.setSelectedWatchers(this, appWidgetId, selected)
                        WidgetPrefs.setShowOnlyActive(this, appWidgetId, onlyActive)
                        WidgetPrefs.setTheme(this, appWidgetId, theme)
                        WidgetRefreshWorker.enqueue(this)
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                        )
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun ConfigContent(
    watchersFlow: Flow<List<Watcher>>,
    initialSelection: Set<Long>,
    initialOnlyActive: Boolean,
    initialTheme: WidgetTheme,
    confirmLabel: String,
    onBack: () -> Unit,
    onConfirm: (List<Long>, Boolean, WidgetTheme) -> Unit,
) {
    val watchers by watchersFlow.collectAsStateWithLifecycle(emptyList())
    var selected by remember { mutableStateOf(initialSelection) }
    var onlyActive by remember { mutableStateOf(initialOnlyActive) }
    var theme by remember { mutableStateOf(initialTheme) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = "Widget",
                    fontSize = 22.sp,
                    fontWeight = FontWeight(700),
                    letterSpacing = (-0.3).sp,
                )
            }
        },
        bottomBar = {
            Button(
                onClick = { onConfirm(selected.toList(), onlyActive, theme) },
                enabled = selected.isNotEmpty(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 14.dp)
                    .height(54.dp),
            ) {
                Text(confirmLabel)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Watchers shown here",
                    fontSize = 13.sp,
                    fontWeight = FontWeight(700),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp),
                )
                WatcherCard(
                    watchers = watchers,
                    selected = selected,
                    onToggle = { watcher, checked ->
                        selected = if (checked) selected - watcher.id else selected + watcher.id
                    },
                )
                Text(
                    // Not "swipeable": a home-screen widget gets no gesture
                    // callbacks, so the arrows and dots are the only way through.
                    text = "Checked watchers become pages you step through with " +
                        "the arrows.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 15.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = "Show only active",
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = onlyActive,
                        onCheckedChange = { onlyActive = it },
                    )
                }
                Text(
                    text = "Resting watchers are skipped in the pager until their " +
                        "window opens.",
                    fontSize = 12.sp,
                    lineHeight = 17.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Widget theme",
                    fontSize = 13.sp,
                    fontWeight = FontWeight(700),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    WidgetTheme.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = theme == option,
                            onClick = { theme = option },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = WidgetTheme.entries.size,
                            ),
                            icon = {},
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = when (option) {
                                        WidgetTheme.SYSTEM -> Icons.Filled.BrightnessAuto
                                        WidgetTheme.LIGHT -> Icons.Filled.LightMode
                                        WidgetTheme.DARK -> Icons.Filled.DarkMode
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    text = when (option) {
                                        WidgetTheme.SYSTEM -> "System"
                                        WidgetTheme.LIGHT -> "Light"
                                        WidgetTheme.DARK -> "Dark"
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight(600),
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Independent of the app — a translucent widget can sit " +
                        "lighter on a bright wallpaper.",
                    fontSize = 12.sp,
                    lineHeight = 17.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun WatcherCard(
    watchers: List<Watcher>,
    selected: Set<Long>,
    onToggle: (Watcher, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        if (watchers.isEmpty()) {
            Text(
                text = "No watchers yet. Create one in Ketch first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 13.dp, horizontal = 15.dp),
            )
        }
        watchers.forEachIndexed { index, watcher ->
            if (index > 0) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            val checked = watcher.id in selected
            val contentAlpha = if (checked) 1f else 0.6f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(watcher, checked) }
                    .padding(vertical = 13.dp, horizontal = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .alpha(contentAlpha)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (checked) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = watcherIcon(watcher.icon),
                        contentDescription = null,
                        tint = if (checked) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = watcher.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(contentAlpha),
                )
            }
        }
    }
}
