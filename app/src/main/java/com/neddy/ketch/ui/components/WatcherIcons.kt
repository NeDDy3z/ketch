package com.neddy.ketch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.neddy.ketch.domain.model.Watcher

/**
 * One entry of the icon catalog. A plain entry is a single glyph; a combined
 * one draws [place] as the backdrop with [icon] badged over it, which is how
 * "home, by car" is built out of the two glyphs rather than a bespoke asset.
 */
data class WatcherIconSpec(
    val key: String,
    val icon: ImageVector,
    val place: ImageVector? = null,
)

/**
 * Catalog of icons a watcher can be tagged with. The key is what gets
 * persisted, so entries must not be renamed.
 */
val watcherIconCatalog: List<WatcherIconSpec> = listOf(
    WatcherIconSpec("train", Icons.Filled.Train),
    WatcherIconSpec("bus", Icons.Filled.DirectionsBus),
    WatcherIconSpec("tram", Icons.Filled.Tram),
    WatcherIconSpec("car", Icons.Filled.DirectionsCar),
    WatcherIconSpec("walk", Icons.AutoMirrored.Filled.DirectionsWalk),
    WatcherIconSpec("home", Icons.Filled.Home),
    WatcherIconSpec("home_car", Icons.Filled.DirectionsCar, place = Icons.Filled.Home),
    WatcherIconSpec("home_walk", Icons.AutoMirrored.Filled.DirectionsWalk, place = Icons.Filled.Home),
    WatcherIconSpec("work", Icons.Filled.Work),
    WatcherIconSpec("work_car", Icons.Filled.DirectionsCar, place = Icons.Filled.Work),
    WatcherIconSpec("work_walk", Icons.AutoMirrored.Filled.DirectionsWalk, place = Icons.Filled.Work),
    WatcherIconSpec("school", Icons.Filled.School),
    WatcherIconSpec("shopping", Icons.Filled.ShoppingCart),
    WatcherIconSpec("gym", Icons.Filled.FitnessCenter),
    WatcherIconSpec("star", Icons.Filled.Star),
    WatcherIconSpec("favorite", Icons.Filled.Favorite),
)

fun watcherIconSpec(key: String): WatcherIconSpec =
    watcherIconCatalog.firstOrNull { it.key == key }
        ?: watcherIconCatalog.first { it.key == Watcher.DEFAULT_ICON }

/** The single vector for callers that cannot draw a combined icon. */
fun watcherIcon(key: String): ImageVector =
    watcherIconSpec(key).let { it.place ?: it.icon }

/** Backdrop share of the tile, leaving room for the badge in the corner. */
private const val PLACE_SCALE = 0.86f
private const val BADGE_DISC_SCALE = 0.62f
private const val BADGE_GLYPH_SCALE = 0.46f

/**
 * Draws a watcher icon at [size]. Combined icons put the transport glyph in a
 * disc of [badgeBackground] — the surface the icon sits on — so the two shapes
 * stay separate instead of merging into one silhouette.
 */
@Composable
fun WatcherIcon(
    iconKey: String,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    badgeBackground: Color = Color.Transparent,
) {
    val spec = watcherIconSpec(iconKey)
    val place = spec.place
    if (place == null) {
        Icon(
            imageVector = spec.icon,
            contentDescription = null,
            tint = tint,
            modifier = modifier.size(size),
        )
        return
    }
    Box(modifier = modifier.size(size)) {
        Icon(
            imageVector = place,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(size * PLACE_SCALE),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(size * BADGE_DISC_SCALE)
                .background(badgeBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = spec.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size * BADGE_GLYPH_SCALE),
            )
        }
    }
}
