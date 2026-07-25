package com.neddy.ketch.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.neddy.ketch.data.settings.ColorPalette

/** Map tile colours for the active palette. See [KetchMapColors]. */
val LocalKetchMapColors: ProvidableCompositionLocal<KetchMapColors> =
    staticCompositionLocalOf { paletteColors(ColorPalette.DEFAULT).map }

/** How long a palette change takes to cross-fade. */
private const val PALETTE_CROSSFADE_MS = 200

@Composable
fun KetchTheme(
    palette: ColorPalette = ColorPalette.DEFAULT,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val tokens = paletteColors(palette)

    // Wallpaper is the shipping default on Android 12+; below that there are no
    // dynamic tones to read, so it falls back to the fixed default seed.
    val target = tokens.scheme
        ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(context)
        } else {
            requireNotNull(paletteColors(ColorPalette.DEFAULT).scheme)
        }

    // Switching palettes is instant with a short cross-fade and no restart.
    val scheme = target.animated()

    val mapColors = if (tokens.scheme == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        remember(scheme) { derivedMapColors(scheme) }
    } else {
        tokens.map
    }

    CompositionLocalProvider(LocalKetchMapColors provides mapColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = KetchShapes,
            content = content,
        )
    }
}

@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable
    fun fade(color: Color) =
        animateColorAsState(color, tween(PALETTE_CROSSFADE_MS), label = "palette").value

    return copy(
        primary = fade(primary),
        onPrimary = fade(onPrimary),
        primaryContainer = fade(primaryContainer),
        onPrimaryContainer = fade(onPrimaryContainer),
        secondary = fade(secondary),
        onSecondary = fade(onSecondary),
        secondaryContainer = fade(secondaryContainer),
        onSecondaryContainer = fade(onSecondaryContainer),
        tertiary = fade(tertiary),
        onTertiary = fade(onTertiary),
        tertiaryContainer = fade(tertiaryContainer),
        onTertiaryContainer = fade(onTertiaryContainer),
        background = fade(background),
        onBackground = fade(onBackground),
        surface = fade(surface),
        onSurface = fade(onSurface),
        surfaceVariant = fade(surfaceVariant),
        onSurfaceVariant = fade(onSurfaceVariant),
        surfaceContainerLowest = fade(surfaceContainerLowest),
        surfaceContainerLow = fade(surfaceContainerLow),
        surfaceContainer = fade(surfaceContainer),
        surfaceContainerHigh = fade(surfaceContainerHigh),
        surfaceContainerHighest = fade(surfaceContainerHighest),
        surfaceDim = fade(surfaceDim),
        surfaceBright = fade(surfaceBright),
        outline = fade(outline),
        outlineVariant = fade(outlineVariant),
        inverseSurface = fade(inverseSurface),
        inverseOnSurface = fade(inverseOnSurface),
        inversePrimary = fade(inversePrimary),
    )
}
