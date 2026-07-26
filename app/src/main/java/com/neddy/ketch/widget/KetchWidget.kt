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
import androidx.glance.text.TextAlign
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
    val icon: String,
    /** Laid out as a departure board when present; the text is the fallback. */
    val journey: WidgetJourney?,
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
private fun innerCardBackground(palette: ColorPalette, theme: WidgetTheme) = run {
    val dark = widgetScheme(palette)
    val light = lightCounterpart(dark)
    when (theme) {
        WidgetTheme.SYSTEM -> ColorProvider(
            day = light.surfaceContainerHighest,
            night = dark.surfaceContainerHighest,
        )
        WidgetTheme.LIGHT -> ColorProvider(
            day = light.surfaceContainerHighest,
            night = light.surfaceContainerHighest,
        )
        WidgetTheme.DARK -> ColorProvider(
            day = dark.surfaceContainerHighest,
            night = dark.surfaceContainerHighest,
        )
    }
}

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
                icon = watcher.icon,
                journey = WidgetPrefs.journey(context, watcher.id),
                connectionLine = WidgetPrefs.connectionLine(context, watcher.id)
                    ?: "Loading...",
            )
        }
        // "Nothing picked" and "everything picked is resting" are different
        // states and must not share a message.
        val allResting = entries.isEmpty() &&
            WidgetPrefs.selectedWatchers(context, appWidgetId).isNotEmpty()
        val refreshing = WidgetPrefs.isRefreshing(context)
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
                    refreshing = refreshing,
                )
            }
        }
    }
}

/**
 * How much of the widget is drawn at the size the user has dragged it to. The
 * widget is resizable down to a 2x1 cell, so each tier drops what no longer
 * fits instead of letting the layout overflow and clip.
 */
private enum class WidgetTier { COMPACT, MEDIUM, FULL }

private fun tierFor(height: Dp): WidgetTier = when {
    height >= 150.dp -> WidgetTier.FULL
    height >= 112.dp -> WidgetTier.MEDIUM
    else -> WidgetTier.COMPACT
}

/** The wordmark is the first thing dropped when the widget is narrow. */
private val TITLE_MIN_WIDTH = 200.dp

/** Below this the journey keeps only its two ends and one chip. */
private val FULL_JOURNEY_MIN_WIDTH = 250.dp

/**
 * Longest line code a chip can carry before it crowds the rail. Every stop takes
 * an equal share of the width, so a three-leg journey leaves each chip about a
 * third of the room a direct one gets and has to give up characters for it.
 */
private fun maxLineCode(legs: Int): Int = if (legs >= 3) 4 else 6

/** Thickness of the track drawn between stops. */
private val RAIL_THICKNESS = 2.dp

/** Breathing room between the rail and the time it runs up to. */
private val RAIL_TEXT_GAP = 4.dp

/** Applies [cornerRadius] only where the platform supports it (API 31+). */
private fun GlanceModifier.roundedCorners(radius: Dp): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) cornerRadius(radius) else this

@Composable
private fun WidgetContent(
    appWidgetId: Int,
    entries: List<WidgetEntry>,
    page: Int,
    palette: ColorPalette,
    theme: WidgetTheme,
    allResting: Boolean,
    refreshing: Boolean,
) {
    val size = LocalSize.current
    val tier = tierFor(size.height)
    val compact = size.width < FULL_JOURNEY_MIN_WIDTH
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(panelBackground(palette, theme))
            .roundedCorners(26.dp)
            .padding(
                top = if (tier == WidgetTier.COMPACT) 8.dp else 14.dp,
                bottom = if (tier == WidgetTier.COMPACT) 8.dp else 11.dp,
            ),
    ) {
        if (tier != WidgetTier.COMPACT) {
            WidgetHeader(
                appWidgetId = appWidgetId,
                showTitle = size.width >= TITLE_MIN_WIDTH,
                refreshing = refreshing,
            )
        }
        Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            if (entries.isEmpty()) {
                EmptyCard(appWidgetId = appWidgetId, allResting = allResting)
            } else {
                WatcherCard(
                    entry = entries[page],
                    palette = palette,
                    theme = theme,
                    tier = tier,
                    compact = compact,
                )
            }
        }
        if (entries.size > 1 && tier != WidgetTier.COMPACT) {
            PagerBar(count = entries.size, page = page)
        }
    }
}

@Composable
private fun WidgetHeader(appWidgetId: Int, showTitle: Boolean, refreshing: Boolean) {
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
        if (refreshing) {
            // Tapping the widget cannot show a spinner, so the label stands in
            // for one until the lookup lands and the widget re-renders.
            Text(
                text = "Refreshing\u2026",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
                maxLines = 1,
                modifier = GlanceModifier.padding(end = 4.dp),
            )
        }
        // A plain clickable box rather than CircleIconButton: the button's own
        // hit area did not line up with what it drew, so taps fell through to
        // the page behind it and opened the app instead of refreshing.
        Box(
            modifier = GlanceModifier
                .size(36.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .roundedCorners(18.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = "Refresh",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(18.dp),
            )
        }
    }
}

/** Opens the widget configuration for this widget instance. */
private fun openWidgetSettings(appWidgetId: Int) = actionStartActivity<WidgetConfigActivity>(
    actionParametersOf(AppWidgetIdKey to appWidgetId),
)

/**
 * One watcher owns the whole page: an identity row with the duration badge, then
 * the journey laid out like a departure board — times on the outside, line chips
 * riding the rail between them, arrival in tertiary.
 */
@Composable
private fun WatcherCard(
    entry: WidgetEntry,
    palette: ColorPalette,
    theme: WidgetTheme,
    tier: WidgetTier,
    compact: Boolean,
) {
    val tight = tier == WidgetTier.COMPACT
    Box(modifier = GlanceModifier.padding(horizontal = if (tight) 10.dp else 15.dp)) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(innerCardBackground(palette, theme))
                .roundedCorners(if (tight) 14.dp else 18.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(
                    vertical = if (tight) 7.dp else 12.dp,
                    horizontal = if (tight) 10.dp else 13.dp,
                ),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The tile is the first thing to go: at a 2x1 cell the name and
                // the duration are what a glance is actually for.
                if (tier == WidgetTier.FULL) {
                    Box(
                        modifier = GlanceModifier
                            .size(30.dp)
                            .background(GlanceTheme.colors.primaryContainer)
                            .roundedCorners(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(widgetIcon(entry.icon)),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                GlanceTheme.colors.onPrimaryContainer,
                            ),
                            modifier = GlanceModifier.size(17.dp),
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(9.dp))
                }
                Text(
                    text = entry.name,
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = if (tight) 12.sp else 13.sp,
                        color = GlanceTheme.colors.onSurface,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                val journey = entry.journey
                if (journey != null) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    DurationBadge(minutes = journey.durationMinutes, tight = tight)
                }
            }
            Spacer(modifier = GlanceModifier.height(if (tight) 5.dp else 11.dp))
            val journey = entry.journey
            if (journey == null) {
                Text(
                    text = entry.connectionLine,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
                    maxLines = if (tight) 1 else 3,
                )
            } else {
                JourneyRow(
                    journey = journey,
                    showStopNames = tier == WidgetTier.FULL,
                    compact = compact,
                    tight = tight,
                )
            }
        }
    }
}

/** The loudest thing on the page, as in the app's cards. */
@Composable
private fun DurationBadge(minutes: Int, tight: Boolean) {
    Box(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.primary)
            .roundedCorners(999.dp)
            .padding(horizontal = if (tight) 7.dp else 9.dp, vertical = if (tight) 2.dp else 3.dp),
    ) {
        Text(
            text = "$minutes min",
            style = TextStyle(
                fontSize = if (tight) 10.sp else 11.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onPrimary,
            ),
            maxLines = 1,
        )
    }
}

/**
 * Times on the outside, chips between. Glance has no absolute positioning, so
 * the rail is drawn as short segments either side of each chip rather than one
 * continuous line behind them.
 *
 * Every stop takes an equal share of the width. Sizing the columns to their own
 * text instead would push a middle stop off centre whenever the two ends differ
 * in length, which they nearly always do.
 */
@Composable
private fun JourneyRow(
    journey: WidgetJourney,
    showStopNames: Boolean,
    compact: Boolean,
    tight: Boolean,
) {
    // Narrow widgets keep only the two ends and a single chip: three stop
    // columns on a 2-cell widget would each be too clipped to read.
    val stops = if (compact) listOf(journey.stops.first(), journey.stops.last()) else journey.stops
    val legs = if (compact) journey.legs.take(1) else journey.legs
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        stops.forEachIndexed { index, stop ->
            val isLast = index == stops.lastIndex
            StopColumn(
                stop = stop,
                isLast = isLast,
                align = when {
                    index == 0 -> TextAlign.Start
                    isLast -> TextAlign.End
                    else -> TextAlign.Center
                },
                // A stop slot is wider than its time, so the rail has to carry on
                // inside the slot as well: without this it stops at the slot edge
                // and leaves a gap before the digits.
                railBefore = index > 0,
                railAfter = !isLast,
                showName = showStopNames,
                tight = tight,
                modifier = GlanceModifier.defaultWeight(),
            )
            if (!isLast) {
                legs.getOrNull(index)?.let { leg ->
                    Row(
                        modifier = GlanceModifier
                            .defaultWeight()
                            // No horizontal padding: the stubs have to meet the
                            // ones inside the stop slots across the boundary.
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RailSegment(modifier = GlanceModifier.defaultWeight())
                        LegChip(
                            leg = leg,
                            more = compact && journey.legs.size > 1,
                            maxCode = maxLineCode(legs.size),
                        )
                        RailSegment(modifier = GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }
}

/**
 * The track either side of a leg chip, tying the stops together. Takes the
 * weight from its caller: `defaultWeight` is Row-scoped and does not survive
 * being moved into a composable of its own.
 */
@Composable
private fun RailSegment(modifier: GlanceModifier) {
    // Square ends on purpose: rounding a 2dp line leaves a visible seam where one
    // stub meets the next across a slot boundary.
    Box(
        modifier = modifier
            .height(RAIL_THICKNESS)
            .background(GlanceTheme.colors.surfaceVariant),
    ) {}
}

@Composable
private fun StopColumn(
    stop: WidgetStop,
    isLast: Boolean,
    align: TextAlign,
    railBefore: Boolean,
    railAfter: Boolean,
    showName: Boolean,
    tight: Boolean,
    modifier: GlanceModifier,
) {
    Column(modifier = modifier) {
        // The stubs take whatever the time leaves, which is also what positions
        // it: right of a leading stub, left of a trailing one, centred between
        // both. So the alignment the name follows comes out of the same layout.
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (railBefore) {
                RailSegment(
                    modifier = GlanceModifier.defaultWeight().padding(end = RAIL_TEXT_GAP),
                )
            }
            Text(
                text = stop.time,
                style = TextStyle(
                    fontSize = if (tight) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLast) {
                        GlanceTheme.colors.tertiary
                    } else {
                        GlanceTheme.colors.onSurface
                    },
                ),
                maxLines = 1,
            )
            if (railAfter) {
                RailSegment(
                    modifier = GlanceModifier.defaultWeight().padding(start = RAIL_TEXT_GAP),
                )
            }
        }
        if (showName) {
            Text(
                text = if (isLast) "arrive" else stop.name,
                style = TextStyle(
                    fontSize = 10.sp,
                    textAlign = align,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
                maxLines = 1,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LegChip(leg: WidgetLeg, more: Boolean, maxCode: Int) {
    Row(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.surfaceVariant)
            .roundedCorners(999.dp)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(vehicleGlyph(leg.vehicleType)),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(11.dp),
        )
        Spacer(modifier = GlanceModifier.width(2.dp))
        Text(
            // Provider line names run long ("Slovak Rail 115"); a chip riding a
            // rail has room for a code, not a sentence.
            text = leg.lineCode.take(maxCode) + if (more) " +" else "",
            style = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurface,
            ),
            maxLines = 1,
        )
    }
}

/**
 * Resource twin of [com.neddy.ketch.ui.components.watcherIconCatalog]. Glance
 * renders in a remote process and can only take drawable resources, so the
 * app's Compose icons cannot be reused directly.
 */
private fun widgetIcon(key: String): Int = when (key) {
    "bus" -> R.drawable.ic_vehicle_bus
    "tram" -> R.drawable.ic_vehicle_tram
    "home" -> R.drawable.ic_watcher_home
    "work" -> R.drawable.ic_watcher_work
    "school" -> R.drawable.ic_watcher_school
    "shopping" -> R.drawable.ic_watcher_shopping
    "gym" -> R.drawable.ic_watcher_gym
    "star" -> R.drawable.ic_watcher_star
    "favorite" -> R.drawable.ic_watcher_favorite
    else -> R.drawable.ic_vehicle_rail
}

private fun vehicleGlyph(vehicleType: String): Int = when (vehicleType.uppercase()) {
    "HEAVY_RAIL", "COMMUTER_TRAIN", "HIGH_SPEED_TRAIN", "LONG_DISTANCE_TRAIN", "RAIL" ->
        R.drawable.ic_vehicle_rail
    "SUBWAY", "METRO_RAIL" -> R.drawable.ic_vehicle_subway
    "TRAM", "LIGHT_RAIL" -> R.drawable.ic_vehicle_tram
    else -> R.drawable.ic_vehicle_bus
}

/**
 * Arrows either side of the page indicators. A home-screen widget cannot receive
 * a swipe — RemoteViews has no pager and no gesture callbacks — so stepping has
 * to be a tap: the arrows move one connection, the dots jump straight to one.
 */
@Composable
private fun PagerBar(count: Int, page: Int) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 2.dp, start = 9.dp, end = 9.dp),
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
 * render they were built in, so the page they move from is read when the click
 * arrives instead of being baked in.
 *
 * A plain clickable box rather than CircleIconButton, for the same reason the
 * refresh button is one — the button's hit area did not line up with what it
 * drew, so taps fell through to the page behind and opened the app.
 */
@Composable
private fun PagerArrow(icon: Int, description: String, step: Int) {
    Box(
        modifier = GlanceModifier
            .size(30.dp)
            .clickable(
                actionRunCallback<StepPageAction>(
                    actionParametersOf(StepPageAction.STEP to step),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            modifier = GlanceModifier.size(18.dp),
        )
    }
}

@Composable
private fun PagerDot(index: Int, active: Boolean) {
    // The active page reads as a short pill, the rest as dots.
    Box(
        // Padding is part of the target: a 6dp dot alone is not tappable.
        modifier = GlanceModifier
            .padding(horizontal = 7.dp, vertical = 5.dp)
            .clickable(showPage(index)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = GlanceModifier
                .size(width = if (active) 16.dp else 6.dp, height = 6.dp)
                .background(
                    if (active) {
                        GlanceTheme.colors.primary
                    } else {
                        GlanceTheme.colors.surfaceVariant
                    },
                )
                .roundedCorners(3.dp),
        ) {}
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
        // Re-render first so the tap is acknowledged, then do the slow part.
        WidgetPrefs.setRefreshing(context, true)
        KetchWidget().update(context, glanceId)
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
