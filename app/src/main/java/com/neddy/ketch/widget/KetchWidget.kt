package com.neddy.ketch.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
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
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.neddy.ketch.MainActivity
import com.neddy.ketch.R
import com.neddy.ketch.appContainer
import com.neddy.ketch.ui.theme.DarkColors
import com.neddy.ketch.ui.theme.LightColors

data class WidgetEntry(
    val watcherId: Long,
    val name: String,
    val connectionLine: String,
)

/** Day/night Glance providers mirroring the app's M3 scheme (docs/design_document.md). */
private val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

/**
 * Panel tone: surfaceContainerLow in light / surfaceContainerHigh in dark.
 * glance-material3 1.1.1 does not expose the surfaceContainer* roles on
 * [GlanceTheme.colors], so this is a hand-built day/night provider using the
 * exact tones from the app color scheme.
 */
private val PanelBackground = ColorProvider(
    day = LightColors.surfaceContainerLow,
    night = DarkColors.surfaceContainerHigh,
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
            GlanceTheme(colors = WidgetColors) {
                WidgetContent(entries)
            }
        }
    }
}

/** Applies [cornerRadius] only where the platform supports it (API 31+). */
private fun GlanceModifier.roundedCorners(radius: Dp): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) cornerRadius(radius) else this

@Composable
private fun WidgetContent(entries: List<WidgetEntry>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(PanelBackground)
            .roundedCorners(26.dp)
            .padding(top = 14.dp, bottom = 13.dp),
    ) {
        WidgetHeader()
        if (entries.isEmpty()) {
            EmptyCard()
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(entries, itemId = { it.watcherId }) { entry ->
                    WatcherCard(entry)
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader() {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(start = 15.dp, end = 15.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(34.dp)
                .background(GlanceTheme.colors.primary)
                .roundedCorners(11.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_logo),
                contentDescription = "Open Ketch",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                modifier = GlanceModifier.size(19.dp),
            )
        }
        Spacer(modifier = GlanceModifier.width(9.dp))
        Text(
            text = "Ketch",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurface,
            ),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        CircleIconButton(
            imageProvider = ImageProvider(R.drawable.ic_widget_refresh),
            contentDescription = "Refresh",
            onClick = actionRunCallback<RefreshWidgetAction>(),
            backgroundColor = GlanceTheme.colors.surfaceVariant,
            contentColor = GlanceTheme.colors.primary,
            modifier = GlanceModifier.size(32.dp),
        )
    }
}

// The cached connection line is a single preformatted string, so there is no
// separate duration value to surface as a badge; the card shows name + line.
@Composable
private fun WatcherCard(entry: WidgetEntry) {
    Box(modifier = GlanceModifier.padding(start = 15.dp, end = 15.dp, bottom = 10.dp)) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .roundedCorners(18.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(vertical = 12.dp, horizontal = 13.dp),
        ) {
            Text(
                text = entry.name,
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = GlanceTheme.colors.onSurface,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
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

@Composable
private fun EmptyCard() {
    Box(modifier = GlanceModifier.padding(horizontal = 15.dp)) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .roundedCorners(18.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(vertical = 12.dp, horizontal = 13.dp),
        ) {
            Text(
                text = "Tap to pick watchers in Ketch",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
            )
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
