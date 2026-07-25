package com.neddy.ketch.widget

import com.neddy.ketch.domain.model.TransitConnection
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val widgetTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Flattens a connection into the shape the widget lays out. */
fun TransitConnection.toWidgetJourney(
    zone: ZoneId = ZoneId.systemDefault(),
): WidgetJourney {
    fun time(instant: java.time.Instant) = widgetTimeFormatter.format(instant.atZone(zone))
    val stops = buildList {
        add(WidgetStop(time(legs.first().departureTime), legs.first().departureStop))
        legs.forEach { add(WidgetStop(time(it.arrivalTime), it.arrivalStop)) }
    }
    return WidgetJourney(
        durationMinutes = travelDuration.toMinutes().toInt(),
        stops = stops,
        legs = legs.map { WidgetLeg(it.lineCode, it.vehicleType) },
    )
}

/** One stop on a widget journey. */
data class WidgetStop(val time: String, val name: String)

/** One leg between two stops, shown as a chip riding the rail. */
data class WidgetLeg(val lineCode: String, val vehicleType: String)

/**
 * A connection flattened for the home-screen widget: the duration badge, the
 * stops in order and the legs between them. There is always one more stop than
 * leg.
 *
 * Glance renders outside the app process and re-reads state on every frame, so
 * this is stored as a single delimited string in [WidgetPrefs] rather than
 * pulling in a serialization library for three fields.
 */
data class WidgetJourney(
    val durationMinutes: Int,
    val stops: List<WidgetStop>,
    val legs: List<WidgetLeg>,
) {
    fun serialize(): String = buildList {
        add(durationMinutes.toString())
        stops.forEachIndexed { index, stop ->
            add("${stop.time}$FIELD${stop.name}")
            legs.getOrNull(index)?.let { add("${it.lineCode}$FIELD${it.vehicleType}") }
        }
    }.joinToString(RECORD)

    companion object {
        // Unit/record separators: never present in a stop name or line code.
        private const val FIELD = "\u001F"
        private const val RECORD = "\u001E"

        fun parse(raw: String): WidgetJourney? {
            val parts = raw.split(RECORD)
            val duration = parts.firstOrNull()?.toIntOrNull() ?: return null
            val stops = mutableListOf<WidgetStop>()
            val legs = mutableListOf<WidgetLeg>()
            parts.drop(1).forEachIndexed { index, part ->
                val fields = part.split(FIELD)
                if (fields.size != 2) return@forEachIndexed
                // Stops and legs alternate, starting with a stop.
                if (index % 2 == 0) {
                    stops += WidgetStop(fields[0], fields[1])
                } else {
                    legs += WidgetLeg(fields[0], fields[1])
                }
            }
            return if (stops.size >= 2) WidgetJourney(duration, stops, legs) else null
        }
    }
}
