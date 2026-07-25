package com.neddy.ketch.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.neddy.ketch.data.settings.ColorPalette

// Eight dark seeds, one tonal system (docs/design_document.md · 00 · COLOR SCHEMES).
// A seed overrides colour roles only — shape, elevation, type and spacing are
// identical across all of them, so a new seed ships without re-checking a layout.
// Ketch is dark-only. The single exception is the home-screen widget, which may
// be asked to sit on a bright wallpaper — see [lightCounterpart].

/**
 * Map tile colours that sit outside the M3 role set. The map picker paints its
 * Google Maps style from these so the pin and radius ring stay the loudest
 * things on screen whichever palette is active.
 */
data class KetchMapColors(
    val land: Color,
    val line: Color,
    val road: Color,
    val water: Color,
)

/**
 * A dark tonal palette plus the map tokens that go with it. [scheme] is null for
 * [ColorPalette.WALLPAPER], which takes its tones from the system at runtime.
 */
data class KetchPaletteColors(
    val scheme: ColorScheme?,
    val map: KetchMapColors,
)

/** The name each palette is offered under in Settings → Appearance. */
val ColorPalette.displayName: String
    get() = when (this) {
        ColorPalette.WALLPAPER -> "Wallpaper"
        ColorPalette.STEEL -> "Night Platform"
        ColorPalette.AURORA -> "Aurora"
        ColorPalette.PHOSPHOR -> "Phosphor"
        ColorPalette.ICE_VIOLET -> "Ice Violet"
        ColorPalette.GRAPHITE -> "Graphite Cyan"
        ColorPalette.AMBER -> "Departure Board"
        ColorPalette.MONO -> "True Black"
    }

/** The one-line description under each palette name. */
val ColorPalette.description: String
    get() = when (this) {
        ColorPalette.WALLPAPER -> "Material You · follows your home screen"
        ColorPalette.STEEL -> "Steel blue"
        ColorPalette.AURORA -> "Indigo periwinkle"
        ColorPalette.PHOSPHOR -> "Departure-board green"
        ColorPalette.ICE_VIOLET -> "Cool lilac"
        ColorPalette.GRAPHITE -> "Neutral graphite"
        ColorPalette.AMBER -> "Warm amber"
        ColorPalette.MONO -> "Greys only · AMOLED"
    }

/**
 * Builds one dark M3 scheme from the seed's tokens. Error roles are shared by
 * every palette — destructive actions read the same everywhere — and
 * `surfaceVariant` follows the outline variant, as in the spec's token blocks.
 */
private fun ketchDarkScheme(
    primary: Long,
    onPrimary: Long,
    primaryContainer: Long,
    onPrimaryContainer: Long,
    secondary: Long,
    onSecondary: Long,
    secondaryContainer: Long,
    onSecondaryContainer: Long,
    tertiary: Long,
    onTertiary: Long,
    tertiaryContainer: Long,
    onTertiaryContainer: Long,
    background: Long,
    onBackground: Long,
    surface: Long,
    onSurface: Long,
    onSurfaceVariant: Long,
    surfaceContainerLowest: Long,
    surfaceContainerLow: Long,
    surfaceContainer: Long,
    surfaceContainerHigh: Long,
    surfaceContainerHighest: Long,
    outline: Long,
    outlineVariant: Long,
    inverseSurface: Long,
    inverseOnSurface: Long,
    inversePrimary: Long,
): ColorScheme = darkColorScheme(
    primary = Color(primary),
    onPrimary = Color(onPrimary),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary),
    onSecondary = Color(onSecondary),
    secondaryContainer = Color(secondaryContainer),
    onSecondaryContainer = Color(onSecondaryContainer),
    tertiary = Color(tertiary),
    onTertiary = Color(onTertiary),
    tertiaryContainer = Color(tertiaryContainer),
    onTertiaryContainer = Color(onTertiaryContainer),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(background),
    onBackground = Color(onBackground),
    surface = Color(surface),
    onSurface = Color(onSurface),
    surfaceVariant = Color(outlineVariant),
    onSurfaceVariant = Color(onSurfaceVariant),
    surfaceContainerLowest = Color(surfaceContainerLowest),
    surfaceContainerLow = Color(surfaceContainerLow),
    surfaceContainer = Color(surfaceContainer),
    surfaceContainerHigh = Color(surfaceContainerHigh),
    surfaceContainerHighest = Color(surfaceContainerHighest),
    surfaceDim = Color(surface),
    surfaceBright = Color(surfaceContainerHighest),
    outline = Color(outline),
    outlineVariant = Color(outlineVariant),
    inverseSurface = Color(inverseSurface),
    inverseOnSurface = Color(inverseOnSurface),
    inversePrimary = Color(inversePrimary),
    scrim = Color(0xFF000000),
)

/** Steel · Night Platform — the cold steel-blue default seed. */
private val SteelColors = ketchDarkScheme(
    primary = 0xFF8ECDFF,
    onPrimary = 0xFF00344F,
    primaryContainer = 0xFF004B70,
    onPrimaryContainer = 0xFFCDE5FF,
    secondary = 0xFFB4C9DA,
    onSecondary = 0xFF1F3341,
    secondaryContainer = 0xFF354A58,
    onSecondaryContainer = 0xFFD0E4F7,
    tertiary = 0xFF7FD6C4,
    onTertiary = 0xFF00382F,
    tertiaryContainer = 0xFF005044,
    onTertiaryContainer = 0xFF9BF2DE,
    background = 0xFF0E1418,
    onBackground = 0xFFE0E4E8,
    surface = 0xFF0E1418,
    onSurface = 0xFFE0E4E8,
    onSurfaceVariant = 0xFFBFC9D3,
    surfaceContainerLowest = 0xFF090D11,
    surfaceContainerLow = 0xFF141A1F,
    surfaceContainer = 0xFF182027,
    surfaceContainerHigh = 0xFF222B32,
    surfaceContainerHighest = 0xFF2D373E,
    outline = 0xFF8A949E,
    outlineVariant = 0xFF414A52,
    inverseSurface = 0xFFE0E4E8,
    inverseOnSurface = 0xFF2A3238,
    inversePrimary = 0xFF00658F,
)

/** Aurora — indigo periwinkle with teal arrivals. */
private val AuroraColors = ketchDarkScheme(
    primary = 0xFFBEC5FF,
    onPrimary = 0xFF242C61,
    primaryContainer = 0xFF3B4279,
    onPrimaryContainer = 0xFFE0E0FF,
    secondary = 0xFFC3C5DD,
    onSecondary = 0xFF2C2F42,
    secondaryContainer = 0xFF434659,
    onSecondaryContainer = 0xFFDFE0F9,
    tertiary = 0xFF8FD7D2,
    onTertiary = 0xFF003736,
    tertiaryContainer = 0xFF00504E,
    onTertiaryContainer = 0xFFABF3EE,
    background = 0xFF121318,
    onBackground = 0xFFE3E1E9,
    surface = 0xFF121318,
    onSurface = 0xFFE3E1E9,
    onSurfaceVariant = 0xFFC6C5D0,
    surfaceContainerLowest = 0xFF0C0E13,
    surfaceContainerLow = 0xFF1A1B21,
    surfaceContainer = 0xFF1E1F25,
    surfaceContainerHigh = 0xFF292A2F,
    surfaceContainerHighest = 0xFF34343A,
    outline = 0xFF90909A,
    outlineVariant = 0xFF46464F,
    inverseSurface = 0xFFE3E1E9,
    inverseOnSurface = 0xFF2F3036,
    inversePrimary = 0xFF535A92,
)

/** Phosphor — departure-board green with ice-blue arrivals. */
private val PhosphorColors = ketchDarkScheme(
    primary = 0xFF7CE0A6,
    onPrimary = 0xFF003920,
    primaryContainer = 0xFF005230,
    onPrimaryContainer = 0xFF98FCC1,
    secondary = 0xFFB4CCBB,
    onSecondary = 0xFF203527,
    secondaryContainer = 0xFF364B3D,
    onSecondaryContainer = 0xFFD0E8D7,
    tertiary = 0xFFA2CEDD,
    onTertiary = 0xFF023641,
    tertiaryContainer = 0xFF204C59,
    onTertiaryContainer = 0xFFBEEAF9,
    background = 0xFF0D1310,
    onBackground = 0xFFDDE5DE,
    surface = 0xFF0D1310,
    onSurface = 0xFFDDE5DE,
    onSurfaceVariant = 0xFFBECABF,
    surfaceContainerLowest = 0xFF080C09,
    surfaceContainerLow = 0xFF131A15,
    surfaceContainer = 0xFF171E19,
    surfaceContainerHigh = 0xFF212823,
    surfaceContainerHighest = 0xFF2C332D,
    outline = 0xFF889389,
    outlineVariant = 0xFF3F4941,
    inverseSurface = 0xFFDDE5DE,
    inverseOnSurface = 0xFF2A322C,
    inversePrimary = 0xFF136B41,
)

/** Ice Violet — cool lilac with mint arrivals. */
private val IceVioletColors = ketchDarkScheme(
    primary = 0xFFD9BDFF,
    onPrimary = 0xFF3F2065,
    primaryContainer = 0xFF57387D,
    onPrimaryContainer = 0xFFEEDBFF,
    secondary = 0xFFCFC1DA,
    onSecondary = 0xFF362C3F,
    secondaryContainer = 0xFF4D4257,
    onSecondaryContainer = 0xFFECDDF7,
    tertiary = 0xFF8AD5C6,
    onTertiary = 0xFF003731,
    tertiaryContainer = 0xFF005047,
    onTertiaryContainer = 0xFFA6F2E2,
    background = 0xFF141218,
    onBackground = 0xFFE7E0E8,
    surface = 0xFF141218,
    onSurface = 0xFFE7E0E8,
    onSurfaceVariant = 0xFFCBC3CF,
    surfaceContainerLowest = 0xFF0E0C11,
    surfaceContainerLow = 0xFF1C1A20,
    surfaceContainer = 0xFF201E25,
    surfaceContainerHigh = 0xFF2B2830,
    surfaceContainerHighest = 0xFF36323B,
    outline = 0xFF958E99,
    outlineVariant = 0xFF4A454E,
    inverseSurface = 0xFFE7E0E8,
    inverseOnSurface = 0xFF312E35,
    inversePrimary = 0xFF6B4E96,
)

/** Graphite Cyan — neutral graphite with an electric cyan accent. */
private val GraphiteColors = ketchDarkScheme(
    primary = 0xFF5FE0E6,
    onPrimary = 0xFF003739,
    primaryContainer = 0xFF004F52,
    onPrimaryContainer = 0xFF8FF6FB,
    secondary = 0xFFB0CBCD,
    onSecondary = 0xFF1B3436,
    secondaryContainer = 0xFF324A4C,
    onSecondaryContainer = 0xFFCCE8EA,
    tertiary = 0xFFC6D0D6,
    onTertiary = 0xFF2A3339,
    tertiaryContainer = 0xFF404950,
    onTertiaryContainer = 0xFFDCE6EC,
    background = 0xFF0F1112,
    onBackground = 0xFFE2E2E3,
    surface = 0xFF0F1112,
    onSurface = 0xFFE2E2E3,
    onSurfaceVariant = 0xFFC2C7C8,
    surfaceContainerLowest = 0xFF0A0B0C,
    surfaceContainerLow = 0xFF161819,
    surfaceContainer = 0xFF1A1D1E,
    surfaceContainerHigh = 0xFF252829,
    surfaceContainerHighest = 0xFF303334,
    outline = 0xFF8C9192,
    outlineVariant = 0xFF424748,
    inverseSurface = 0xFFE2E2E3,
    inverseOnSurface = 0xFF2D3031,
    inversePrimary = 0xFF00696E,
)

/** Departure Board — warm amber with olive arrivals. */
private val AmberColors = ketchDarkScheme(
    primary = 0xFFFFB868,
    onPrimary = 0xFF4A2800,
    primaryContainer = 0xFF693C00,
    onPrimaryContainer = 0xFFFFDCBE,
    secondary = 0xFFE4BFA6,
    onSecondary = 0xFF432B1B,
    secondaryContainer = 0xFF5B4130,
    onSecondaryContainer = 0xFFFFDCBE,
    tertiary = 0xFFC1CC9A,
    onTertiary = 0xFF2B3410,
    tertiaryContainer = 0xFF414B24,
    onTertiaryContainer = 0xFFDDE8B4,
    background = 0xFF19120C,
    onBackground = 0xFFEEE0D4,
    surface = 0xFF19120C,
    onSurface = 0xFFEEE0D4,
    onSurfaceVariant = 0xFFD6C5B6,
    surfaceContainerLowest = 0xFF130D07,
    surfaceContainerLow = 0xFF211A13,
    surfaceContainer = 0xFF251E17,
    surfaceContainerHigh = 0xFF302821,
    surfaceContainerHighest = 0xFF3B322B,
    outline = 0xFF9F8F80,
    outlineVariant = 0xFF51443B,
    inverseSurface = 0xFFEEE0D4,
    inverseOnSurface = 0xFF372F27,
    inversePrimary = 0xFF8A5100,
)

/** True Black — pure black and greys only, for AMOLED night glances. */
private val MonoColors = ketchDarkScheme(
    primary = 0xFFE8E8E8,
    onPrimary = 0xFF161616,
    primaryContainer = 0xFF2E2E2E,
    onPrimaryContainer = 0xFFF5F5F5,
    secondary = 0xFFC2C2C2,
    onSecondary = 0xFF1C1C1C,
    secondaryContainer = 0xFF303030,
    onSecondaryContainer = 0xFFEDEDED,
    tertiary = 0xFFFFFFFF,
    onTertiary = 0xFF000000,
    tertiaryContainer = 0xFF3A3A3A,
    onTertiaryContainer = 0xFFFFFFFF,
    background = 0xFF000000,
    onBackground = 0xFFEDEDED,
    surface = 0xFF000000,
    onSurface = 0xFFEDEDED,
    onSurfaceVariant = 0xFFB5B5B5,
    surfaceContainerLowest = 0xFF000000,
    surfaceContainerLow = 0xFF0A0A0A,
    surfaceContainer = 0xFF121212,
    surfaceContainerHigh = 0xFF1C1C1C,
    surfaceContainerHighest = 0xFF262626,
    outline = 0xFF8A8A8A,
    outlineVariant = 0xFF3A3A3A,
    inverseSurface = 0xFFEDEDED,
    inverseOnSurface = 0xFF2A2A2A,
    inversePrimary = 0xFF4A4A4A,
)

/**
 * A light rendering of a dark seed, for the one surface that may be asked to sit
 * on a bright wallpaper: the home-screen widget. The app itself is dark-only.
 *
 * The spec gives no light tokens, so nothing here is invented — every role is
 * read back out of the dark scheme using M3's own tonal symmetry, which the
 * amber seed's original light scheme confirms exactly:
 *
 *  - `inversePrimary` in a dark scheme *is* the light primary (T40).
 *  - a dark `onPrimaryContainer` (T90) is the light `primaryContainer`.
 *  - a dark `onPrimary` (T20) is dark enough to read on that T90 container.
 *  - `inverseSurface` / `inverseOnSurface` are the light surface and its text.
 *
 * Only the roles the widget actually paints are mapped; the rest fall out of
 * [lightColorScheme]'s defaults and are never drawn.
 */
fun lightCounterpart(dark: ColorScheme): ColorScheme = lightColorScheme(
    primary = dark.inversePrimary,
    onPrimary = Color.White,
    primaryContainer = dark.onPrimaryContainer,
    onPrimaryContainer = dark.onPrimary,
    secondary = dark.secondaryContainer,
    onSecondary = Color.White,
    secondaryContainer = dark.onSecondaryContainer,
    onSecondaryContainer = dark.onSecondary,
    tertiary = dark.tertiaryContainer,
    onTertiary = Color.White,
    tertiaryContainer = dark.onTertiaryContainer,
    onTertiaryContainer = dark.onTertiary,
    background = dark.inverseSurface,
    onBackground = dark.inverseOnSurface,
    surface = dark.inverseSurface,
    onSurface = dark.inverseOnSurface,
    surfaceVariant = dark.onSurfaceVariant,
    onSurfaceVariant = dark.inverseOnSurface,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = dark.inverseSurface,
    surfaceContainer = dark.inverseSurface,
    surfaceContainerHigh = dark.inverseSurface,
    surfaceContainerHighest = dark.onSurfaceVariant,
    outline = dark.outline,
    outlineVariant = dark.onSurfaceVariant,
    inverseSurface = dark.inverseOnSurface,
    inverseOnSurface = dark.inverseSurface,
    inversePrimary = dark.primary,
    scrim = Color(0xFF000000),
)

private fun mapColors(land: Long, line: Long, road: Long, water: Long) = KetchMapColors(
    land = Color(land),
    line = Color(line),
    road = Color(road),
    water = Color(water),
)

/**
 * Tokens for [palette]. Wallpaper carries no fixed scheme — [KetchTheme] swaps in
 * the system's dynamic tones and derives matching map colours from them.
 */
fun paletteColors(palette: ColorPalette): KetchPaletteColors = when (palette) {
    ColorPalette.WALLPAPER -> KetchPaletteColors(
        scheme = null,
        map = mapColors(0xFF17151C, 0xFF272430, 0xFF201D27, 0xFF232840),
    )
    ColorPalette.STEEL -> KetchPaletteColors(
        scheme = SteelColors,
        map = mapColors(0xFF121A20, 0xFF212C34, 0xFF1A232A, 0xFF14303F),
    )
    ColorPalette.AURORA -> KetchPaletteColors(
        scheme = AuroraColors,
        map = mapColors(0xFF161720, 0xFF25262F, 0xFF1E1F28, 0xFF1C2340),
    )
    ColorPalette.PHOSPHOR -> KetchPaletteColors(
        scheme = PhosphorColors,
        map = mapColors(0xFF111814, 0xFF1F2A22, 0xFF182019, 0xFF123037),
    )
    ColorPalette.ICE_VIOLET -> KetchPaletteColors(
        scheme = IceVioletColors,
        map = mapColors(0xFF191620, 0xFF28242F, 0xFF211D28, 0xFF232841),
    )
    ColorPalette.GRAPHITE -> KetchPaletteColors(
        scheme = GraphiteColors,
        map = mapColors(0xFF131617, 0xFF232728, 0xFF1B1F20, 0xFF12333A),
    )
    ColorPalette.AMBER -> KetchPaletteColors(
        scheme = AmberColors,
        map = mapColors(0xFF241E17, 0xFF332B22, 0xFF2E2820, 0xFF20303A),
    )
    ColorPalette.MONO -> KetchPaletteColors(
        scheme = MonoColors,
        map = mapColors(0xFF0B0B0B, 0xFF1F1F1F, 0xFF161616, 0xFF1A1A1A),
    )
}

/**
 * The three tones each palette is previewed by in Settings — primary, tertiary
 * and the card surface — rather than an abstract swatch.
 */
fun paletteSwatch(palette: ColorPalette, scheme: ColorScheme): List<Color> =
    paletteColors(palette).scheme.let { fixed ->
        val source = fixed ?: scheme
        listOf(source.primary, source.tertiary, source.surfaceContainer)
    }

/**
 * Map tiles for a wallpaper-derived scheme: the surfaces already track the
 * wallpaper, and water leans on primary so the ring still reads against it.
 */
fun derivedMapColors(scheme: ColorScheme): KetchMapColors = KetchMapColors(
    land = scheme.surfaceContainerLow,
    line = scheme.surfaceContainerHigh,
    road = scheme.surfaceContainer,
    water = scheme.primary.copy(alpha = 0.22f).compositeOverOpaque(scheme.surfaceContainer),
)

/** Flattens [this] over an opaque [background], since map styles need solid colours. */
private fun Color.compositeOverOpaque(background: Color): Color = Color(
    red = red * alpha + background.red * (1f - alpha),
    green = green * alpha + background.green * (1f - alpha),
    blue = blue * alpha + background.blue * (1f - alpha),
    alpha = 1f,
)
