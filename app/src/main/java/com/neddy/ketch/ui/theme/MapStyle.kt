package com.neddy.ketch.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * A Google Maps style built from the active palette's map tokens rather than a
 * fixed JSON file, so the tiles re-tint with the rest of the app.
 *
 * The tiles are deliberately a desaturated ink palette: the pin, radius ring and
 * confirm button are the only saturated things on the screen, and they should
 * read first.
 */
fun ketchMapStyleJson(
    map: KetchMapColors,
    onSurface: Color,
    onSurfaceVariant: Color,
): String {
    val land = map.land.hex()
    val line = map.line.hex()
    val road = map.road.hex()
    val water = map.water.hex()
    val label = onSurfaceVariant.hex()
    val strongLabel = onSurface.hex()

    fun rule(feature: String?, element: String, color: String): String {
        val featurePart = feature?.let { "\"featureType\":\"$it\"," } ?: ""
        return """{$featurePart"elementType":"$element","stylers":[{"color":"$color"}]}"""
    }

    // POI and transit labels are hidden outright — a stop name repeated by the
    // basemap competes with the sheet that already names the picked place.
    val hidden = listOf("poi.business", "transit.station", "poi.attraction")
        .joinToString(",") {
            """{"featureType":"$it","elementType":"labels","stylers":[{"visibility":"off"}]}"""
        }

    return listOf(
        rule(null, "geometry", land),
        rule(null, "labels.text.fill", label),
        rule(null, "labels.text.stroke", land),
        rule("administrative", "geometry.stroke", line),
        rule("administrative.locality", "labels.text.fill", strongLabel),
        rule("landscape.man_made", "geometry", land),
        rule("poi.park", "geometry", line),
        rule("poi", "labels.text.fill", label),
        rule("road", "geometry", road),
        rule("road", "geometry.stroke", line),
        rule("road", "labels.text.fill", label),
        rule("road.highway", "geometry", line),
        rule("road.highway", "geometry.stroke", road),
        rule("transit", "geometry", line),
        rule("transit.line", "geometry", line),
        rule("water", "geometry", water),
        rule("water", "labels.text.fill", label),
        hidden,
    ).joinToString(",", prefix = "[", postfix = "]")
}

/** `#RRGGBB`, the only colour form the Maps styler accepts. */
private fun Color.hex(): String = "#%06X".format(toArgb() and 0xFFFFFF)
