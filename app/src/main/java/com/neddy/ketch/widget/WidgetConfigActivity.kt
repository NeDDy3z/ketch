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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.neddy.ketch.data.settings.ThemeMode
import com.neddy.ketch.domain.model.Watcher
import com.neddy.ketch.ui.components.watcherIcon
import com.neddy.ketch.ui.theme.KetchTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Configuration shown when the widget is dropped on the home screen. The
 * user picks which watchers the widget cycles through.
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
        val themeModeFlow = container.settingsRepository.settings.map { it.themeMode }
        val watchersFlow = container.watcherRepository.observeWatchers()
        val initialSelection = WidgetPrefs.selectedWatchers(this, appWidgetId).toSet()

        setContent {
            val themeMode by themeModeFlow.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            KetchTheme(themeMode = themeMode) {
                ConfigContent(
                    watchersFlow = watchersFlow,
                    initialSelection = initialSelection,
                    onBack = { finish() },
                    onConfirm = { selected ->
                        WidgetPrefs.setSelectedWatchers(this, appWidgetId, selected)
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
    onBack: () -> Unit,
    onConfirm: (List<Long>) -> Unit,
) {
    val watchers by watchersFlow.collectAsStateWithLifecycle(emptyList())
    var selected by remember { mutableStateOf(initialSelection) }

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
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 14.dp)
                    .height(54.dp),
            ) {
                Text("Add widget")
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
                    text = "Checked watchers appear in the widget.",
                    fontSize = 12.sp,
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
