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
 * Title: "16:00 Praha hl.n. (R41) [train emoji]"
 * Body:  "16:30 Cesky Brod (660) [bus emoji]"
 *        "17:00 K.n.C.l, nam."
 *
 * Every boarding is rendered as "time stop (line) emoji" and the final line
 * is "time stop". Walking segments are excluded by design. Known verbose
 * stop names are shortened via [stopNameOverrides].
 */
object ConnectionFormatter {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Verbose provider stop names mapped to the preferred short forms. */
    private val stopNameOverrides = mapOf(
        "Kostelec n.Č.L.,Nám." to "K.n.Č.l, nám.",
        "Hlavní nádraží" to "Praha hl.n.",
        "I. P. Pavlova" to "I.P. Pavlova",
        "Masarykovo nádraží" to "Masarykovo n.",
    )

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
        val arrival = "${time(last.arrivalTime, zoneId)} ${stopName(last.arrivalStop)}"
        return (transfers + arrival).joinToString(separator)
    }

    private fun boarding(leg: TransitLeg, zoneId: ZoneId): String =
        "${time(leg.departureTime, zoneId)} ${stopName(leg.departureStop)} " +
            "(${leg.lineCode}) ${vehicleEmoji(leg.vehicleType)}"

    private fun stopName(name: String): String = stopNameOverrides[name.trim()] ?: name

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
