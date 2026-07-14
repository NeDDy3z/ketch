package com.neddy.ketch.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.neddy.ketch.domain.model.Watcher

/**
 * Catalog of icons a watcher can be tagged with. The key is what gets
 * persisted, so entries must not be renamed.
 */
val watcherIconCatalog: List<Pair<String, ImageVector>> = listOf(
    "train" to Icons.Filled.Train,
    "bus" to Icons.Filled.DirectionsBus,
    "tram" to Icons.Filled.Tram,
    "home" to Icons.Filled.Home,
    "work" to Icons.Filled.Work,
    "school" to Icons.Filled.School,
    "shopping" to Icons.Filled.ShoppingCart,
    "gym" to Icons.Filled.FitnessCenter,
    "star" to Icons.Filled.Star,
    "favorite" to Icons.Filled.Favorite,
)

fun watcherIcon(key: String): ImageVector =
    watcherIconCatalog.firstOrNull { it.first == key }?.second
        ?: watcherIconCatalog.first { it.first == Watcher.DEFAULT_ICON }.second
