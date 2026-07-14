package com.neddy.ketch.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neddy.ketch.MainActivity
import com.neddy.ketch.R
import com.neddy.ketch.appContainer

data class WidgetEntry(
    val watcherId: Long,
    val name: String,
    val connectionLine: String,
)

/**
 * Home screen widget showing the current fastest connection for the
 * watchers picked in the widget configuration. The list scrolls and the
 * refresh button re-fetches all lines.
 */
class KetchWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val watcherIds = WidgetPrefs.selectedWatchers(context, appWidgetId)
        val repository = context.appContainer.watcherRepository
        val entries = watcherIds.mapNotNull { watcherId ->
            repository.getWatcher(watcherId)?.let { watcher ->
                WidgetEntry(
                    watcherId = watcherId,
                    name = watcher.name,
                    connectionLine = WidgetPrefs.connectionLine(context, watcherId)
                        ?: "Loading...",
                )
            }
        }
        provideContent {
            GlanceTheme {
                WidgetContent(entries)
            }
        }
    }
}

@Composable
private fun WidgetContent(entries: List<WidgetEntry>) {
    var background = GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.widgetBackground)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        background = background.cornerRadius(20.dp)
    }
    Column(modifier = background.padding(12.dp)) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ketch",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GlanceTheme.colors.onSurface,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            CircleIconButton(
                imageProvider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = "Refresh",
                onClick = actionRunCallback<RefreshWidgetAction>(),
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        if (entries.isEmpty()) {
            Text(
                text = "Tap to pick watchers in Ketch",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity<MainActivity>()),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(entries, itemId = { it.watcherId }) { entry ->
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(actionStartActivity<MainActivity>()),
                    ) {
                        Text(
                            text = entry.name,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GlanceTheme.colors.onSurface,
                            ),
                            maxLines = 1,
                        )
                        Text(
                            text = entry.connectionLine,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onSurfaceVariant,
                            ),
                            maxLines = 3,
                        )
                    }
                }
            }
        }
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        WidgetRefreshWorker.enqueue(context)
    }
}
