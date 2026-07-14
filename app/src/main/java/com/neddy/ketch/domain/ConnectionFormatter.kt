package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.TransitLeg
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formats a connection for the notification. The title carries the first
 * boarding, the body continues with the remaining boardings and the arrival:
 *
 * Title: "[train emoji] Praha hl.n. (R41) 16:00"
 * Body:  "[bus emoji] Cesky Brod (660) 16:30"
 *        "Kostelec n.C. lesy 17:00"
 *
 * Every boarding is rendered as "stop (line) departure" prefixed with the
 * vehicle emoji, and the final line is "stop arrival". Walking segments are
 * excluded by design.
 */
object ConnectionFormatter {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun notificationTitle(
        connection: TransitConnection,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String = boarding(connection.legs.first(), zoneId)

    /** Collapsed one line body: everything after the first boarding. */
    fun notificationText(
        connection: TransitConnection,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String = continuation(connection, zoneId, separator = " - ")

    /** Expanded body: everything after the first boarding, one stop per line. */
    fun notificationBigText(
        connection: TransitConnection,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String = continuation(connection, zoneId, separator = "\n")

    private fun continuation(
        connection: TransitConnection,
        zoneId: ZoneId,
        separator: String,
    ): String {
        val transfers = connection.legs.drop(1).map { boarding(it, zoneId) }
        val last = connection.legs.last()
        val arrival = "${last.arrivalStop} ${time(last.arrivalTime, zoneId)}"
        return (transfers + arrival).joinToString(separator)
    }

    private fun boarding(leg: TransitLeg, zoneId: ZoneId): String =
        "${vehicleEmoji(leg.vehicleType)} ${leg.departureStop} (${leg.lineCode}) " +
            time(leg.departureTime, zoneId)

    private fun vehicleEmoji(vehicleType: String): String = when (vehicleType.uppercase()) {
        "HEAVY_RAIL", "COMMUTER_TRAIN", "HIGH_SPEED_TRAIN", "LONG_DISTANCE_TRAIN", "RAIL",
        "METRO_RAIL", "MONORAIL",
        -> "🚆"
        "SUBWAY" -> "🚇"
        "TRAM", "LIGHT_RAIL" -> "🚊"
        "FERRY" -> "⛴️"
        "CABLE_CAR", "GONDOLA_LIFT", "FUNICULAR" -> "🚡"
        else -> "🚌"
    }

    private fun time(instant: Instant, zoneId: ZoneId): String =
        timeFormatter.format(instant.atZone(zoneId))
}
