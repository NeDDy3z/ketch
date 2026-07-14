package com.neddy.ketch.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neddy.ketch.appContainer
import com.neddy.ketch.data.settings.ThemeMode
import com.neddy.ketch.ui.components.watcherIcon
import com.neddy.ketch.ui.theme.KetchTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun ConfigContent(
    watchersFlow: kotlinx.coroutines.flow.Flow<List<com.neddy.ketch.domain.model.Watcher>>,
    initialSelection: Set<Long>,
    onConfirm: (List<Long>) -> Unit,
) {
    val watchers by watchersFlow.collectAsStateWithLifecycle(emptyList())
    var selected by remember { mutableStateOf(initialSelection) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget watchers", fontWeight = FontWeight.Bold) },
            )
        },
        bottomBar = {
            Button(
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("Add widget")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (watchers.isEmpty()) {
                item {
                    Text(
                        text = "No watchers yet. Create one in Ketch first.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(watchers, key = { it.id }) { watcher ->
                val checked = watcher.id in selected
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = watcherIcon(watcher.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = { Text(watcher.name) },
                    supportingContent = { Text("To ${watcher.destination.name}") },
                    trailingContent = {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (checked) selected - watcher.id else selected + watcher.id
                        },
                )
            }
        }
    }
}
