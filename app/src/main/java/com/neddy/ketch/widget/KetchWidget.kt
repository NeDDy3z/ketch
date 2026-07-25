package com.neddy.ketch.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import com.neddy.ketch.data.settings.ColorPalette
import com.neddy.ketch.domain.model.Watcher
import com.neddy.ketch.ui.theme.lightCounterpart
import com.neddy.ketch.ui.theme.paletteColors
import java.time.LocalDateTime

data class WidgetEntry(
    val watcherId: Long,
    val name: String,
    val connectionLine: String,
)

/**
 * The dark scheme behind a palette. Glance renders outside the app's
 * composition, so wallpaper palettes have no dynamic tones to read here and
 * fall back to the default seed.
 */
private fun widgetScheme(palette: ColorPalette) =
    paletteColors(palette).scheme
        ?: requireNotNull(paletteColors(ColorPalette.DEFAULT).scheme)

/**
 * Glance providers for the palette, resolved through the widget's own theme.
 * The app is dark-only, but a placed widget may need to sit lighter on a bright
 * wallpaper, so [WidgetTheme.LIGHT] paints the palette's light counterpart and
 * [WidgetTheme.SYSTEM] hands both to Glance to pick per the device setting.
 */
private fun widgetColors(palette: ColorPalette, theme: WidgetTheme) = run {
    val dark = widgetScheme(palette)
    val light = lightCounterpart(dark)
    when (theme) {
        WidgetTheme.SYSTEM -> ColorProviders(light = light, dark = dark)
        WidgetTheme.LIGHT -> ColorProviders(light = light, dark = light)
        WidgetTheme.DARK -> ColorProviders(light = dark, dark = dark)
    }
}

/**
 * Panel tone. glance-material3 1.1.1 does not expose the surfaceContainer*
 * roles on [GlanceTheme.colors], so the raised panel takes its tone straight
 * from the resolved scheme — a widget must lift off arbitrary wallpaper.
 */
private fun panelBackground(palette: ColorPalette, theme: WidgetTheme) = run {
    val dark = widgetScheme(palette)
    val light = lightCounterpart(dark)
    when (theme) {
        WidgetTheme.SYSTEM -> ColorProvider(
            day = light.surfaceContainerHigh,
            night = dark.surfaceContainerHigh,
        )
        WidgetTheme.LIGHT -> ColorProvider(
            day = light.surfaceContainerHigh,
            night = light.surfaceContainerHigh,
        )
        WidgetTheme.DARK -> ColorProvider(
            day = dark.surfaceContainerHigh,
            night = dark.surfaceContainerHigh,
        )
    }
}

/** Widget id carried to [WidgetConfigActivity] when opening it from the widget. */
private val AppWidgetIdKey = ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID)

/**
 * Home screen widget showing the current fastest connection for the watchers
 * picked in the widget configuration. One connection fills the panel at a
 * time; the dots and arrows at the bottom page between them, the logo tile
 * opens the widget configuration and the refresh button re-fetches all lines.
 */
class KetchWidget : GlanceAppWidget() {

    // Exact so the layout follows the user's resize instead of being rendered
    // once for the smallest cell size.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val palette = context.appContainer.settingsRepository.current().palette
        val theme = WidgetPrefs.theme(context, appWidgetId)
        // Resting watchers are skipped in the loop rather than shown empty, so
        // a two-watcher evening cycles between two pages, not five.
        val entries = pagedWatchers(context, appWidgetId).map { watcher ->
            WidgetEntry(
                watcherId = watcher.id,
                name = watcher.name,
                connectionLine = WidgetPrefs.connectionLine(context, watcher.id)
                    ?: "Loading...",
            )
        }
        // "Nothing picked" and "everything picked is resting" are different
        // states and must not share a message.
        val allResting = entries.isEmpty() &&
            WidgetPrefs.selectedWatchers(context, appWidgetId).isNotEmpty()
        // A watcher can disappear between two renders, so the stored page is
        // clamped instead of trusted.
        val page = WidgetPrefs.page(context, appWidgetId)
            .coerceIn(0, maxOf(0, entries.lastIndex))
        provideContent {
            GlanceTheme(colors = widgetColors(palette, theme)) {
                WidgetContent(
                    appWidgetId = appWidgetId,
                    entries = entries,
                    page = page,
                    palette = palette,
                    theme = theme,
                    allResting = allResting,
                )
            }
        }
    }
}

/** Applies [cornerRadius] only where the platform supports it (API 31+). */
private fun GlanceModifier.roundedCorners(radius: Dp): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) cornerRadius(radius) else this

/**
 * How many journey lines fit next to the header and the pager bar. Header,
 * pager and panel padding take about 104dp, every journey line about 17dp.
 */
private fun journeyLines(size: DpSize): Int =
    ((size.height - 104.dp) / 17.dp).toInt().coerceIn(1, 8)

@Composable
private fun WidgetContent(
    appWidgetId: Int,
    entries: List<WidgetEntry>,
    page: Int,
    palette: ColorPalette,
    theme: WidgetTheme,
    allResting: Boolean,
) {
    val size = LocalSize.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(panelBackground(palette, theme))
            .roundedCorners(26.dp)
            .padding(top = 14.dp, bottom = 11.dp),
    ) {
        // The wordmark is the first thing dropped when the widget is narrow.
        WidgetHeader(appWidgetId = appWidgetId, showTitle = size.width >= 200.dp)
        Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            if (entries.isEmpty()) {
                EmptyCard(appWidgetId = appWidgetId, allResting = allResting)
            } else {
                WatcherCard(entry = entries[page], journeyLines = journeyLines(size))
            }
        }
        if (entries.size > 1) {
            PagerBar(count = entries.size, page = page)
        }
    }
}

@Composable
private fun WidgetHeader(appWidgetId: Int, showTitle: Boolean) {
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
                .clickable(openWidgetSettings(appWidgetId)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_logo),
                contentDescription = "Widget settings",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                modifier = GlanceModifier.size(19.dp),
            )
        }
        if (showTitle) {
            Spacer(modifier = GlanceModifier.width(9.dp))
            Text(
                text = "Ketch",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onSurface,
                ),
                maxLines = 1,
            )
        }
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

/** Opens the widget configuration for this widget instance. */
private fun openWidgetSettings(appWidgetId: Int) = actionStartActivity<WidgetConfigActivity>(
    actionParametersOf(AppWidgetIdKey to appWidgetId),
)

// The cached connection line is a preformatted block, one boarding per line,
// so there is no separate duration value to surface as a badge.
@Composable
private fun WatcherCard(entry: WidgetEntry, journeyLines: Int) {
    Box(modifier = GlanceModifier.padding(horizontal = 15.dp)) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
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
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = entry.connectionLine,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
                maxLines = journeyLines,
            )
        }
    }
}

/**
 * Pager controls for the connections: chevrons on the edges wrapping around,
 * dots in the middle that jump straight to a connection.
 */
@Composable
private fun PagerBar(count: Int, page: Int) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 6.dp, start = 9.dp, end = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PagerArrow(
            icon = R.drawable.ic_widget_chevron_left,
            description = "Previous connection",
            step = -1,
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(count) { index -> PagerDot(index = index, active = index == page) }
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
        PagerArrow(
            icon = R.drawable.ic_widget_chevron_right,
            description = "Next connection",
            step = 1,
        )
    }
}

/**
 * The arrows carry a step, not a target page: the click intents outlive the
 * render they were built in, so the page they move from is read when the
 * click arrives instead of being baked in.
 */
@Composable
private fun PagerArrow(icon: Int, description: String, step: Int) {
    CircleIconButton(
        imageProvider = ImageProvider(icon),
        contentDescription = description,
        onClick = actionRunCallback<StepPageAction>(
            actionParametersOf(StepPageAction.STEP to step),
        ),
        backgroundColor = null,
        contentColor = GlanceTheme.colors.onSurfaceVariant,
        modifier = GlanceModifier.size(28.dp),
    )
}

@Composable
private fun PagerDot(index: Int, active: Boolean) {
    Box(
        modifier = GlanceModifier
            .padding(horizontal = 4.dp, vertical = 7.dp)
            .clickable(showPage(index)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_dot),
            contentDescription = "Connection ${index + 1}",
            colorFilter = ColorFilter.tint(
                if (active) GlanceTheme.colors.primary else GlanceTheme.colors.outline,
            ),
            modifier = GlanceModifier.size(if (active) 8.dp else 6.dp),
        )
    }
}

private fun showPage(page: Int) = actionRunCallback<ShowPageAction>(
    actionParametersOf(ShowPageAction.PAGE to page),
)

/**
 * The watchers a widget actually pages through, in the configured order.
 * With "Show only active" on, resting ones drop out of the loop entirely.
 */
private suspend fun pagedWatchers(context: Context, appWidgetId: Int): List<Watcher> {
    val repository = context.appContainer.watcherRepository
    val onlyActive = WidgetPrefs.showOnlyActive(context, appWidgetId)
    val now = LocalDateTime.now()
    return WidgetPrefs.selectedWatchers(context, appWidgetId).mapNotNull { watcherId ->
        repository.getWatcher(watcherId)
            ?.takeIf { !onlyActive || (it.enabled && it.isActiveAt(now)) }
    }
}

/** Number of connections a widget pages through. */
private suspend fun pageCount(context: Context, appWidgetId: Int): Int =
    pagedWatchers(context, appWidgetId).size

@Composable
private fun EmptyCard(appWidgetId: Int, allResting: Boolean) {
    Box(modifier = GlanceModifier.padding(horizontal = 15.dp)) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .roundedCorners(18.dp)
                .clickable(openWidgetSettings(appWidgetId))
                .padding(vertical = 12.dp, horizontal = 13.dp),
        ) {
            Text(
                // Picked-but-all-resting is not the same as nothing picked, and
                // telling someone to pick watchers they already picked is a
                // dead end.
                text = if (allResting) {
                    "Every watcher is resting — the pager resumes when a window opens"
                } else {
                    "Tap to pick watchers for this widget"
                },
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
        parameters: ActionParameters,
    ) {
        WidgetRefreshWorker.enqueue(context)
    }
}

/** Jumps the widget's connection pager straight to a page, used by the dots. */
class ShowPageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val page = parameters[PAGE] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val count = pageCount(context, appWidgetId)
        if (page !in 0 until count) return
        WidgetPrefs.setPage(context, appWidgetId, page)
        KetchWidget().update(context, glanceId)
    }

    companion object {
        val PAGE = ActionParameters.Key<Int>("page")
    }
}

/** Steps the widget's connection pager one page, wrapping around at the ends. */
class StepPageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val step = parameters[STEP] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val count = pageCount(context, appWidgetId)
        if (count <= 1) return
        val current = WidgetPrefs.page(context, appWidgetId).coerceIn(0, count - 1)
        WidgetPrefs.setPage(context, appWidgetId, (current + step + count) % count)
        KetchWidget().update(context, glanceId)
    }

    companion object {
        val STEP = ActionParameters.Key<Int>("step")
    }
}
