package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.TransitLeg
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formats a connection for the notification. Ketch owns exactly two strings:
 * the title is the watcher's own name, and the body carries the whole decision
 * so it is actionable straight from the lock screen without opening the app.
 *
 * Body:      "16:00 Praha hl.n. (R41) [train] → arrives 17:00 · 1 transfer"
 * Expanded:  "16:00 Praha hl.n. (R41) [train]"
 *            "16:30 Cesky Brod (660) [bus]"
 *            "17:00 K.n.C.l, nam."
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

    /**
     * The collapsed one-line body: departure, stop, line, arrival and transfer
     * count. When [now] is given and the first departure is still ahead, a
     * "leave within N min" countdown is appended.
     */
    fun notificationText(
        connection: TransitConnection,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: Instant? = null,
        driveAfter: Boolean = false,
    ): String {
        val first = connection.legs.first()
        val last = connection.legs.last()
        val parts = mutableListOf(
            "${boarding(first, zoneId)} → arrives ${time(last.arrivalTime, zoneId)}",
            transfersText(connection.legs.size - 1),
        )
        leaveWithin(first.departureTime, now)?.let(parts::add)
        // The arrival is the car, not the door, so say the journey goes on.
        if (driveAfter) parts.add("then drive")
        return parts.joinToString(" · ")
    }

    /**
     * Expanded body: every boarding and the arrival, one stop per line. A
     * drivable leg is a line of its own either side of the transit ones, so the
     * whole door-to-door journey reads in order.
     */
    fun notificationBigText(
        connection: TransitConnection,
        zoneId: ZoneId = ZoneId.systemDefault(),
        driveBefore: String? = null,
        driveAfter: String? = null,
    ): String {
        val boardings = connection.legs.map { boarding(it, zoneId) }
        val last = connection.legs.last()
        val arrival = "${time(last.arrivalTime, zoneId)} ${stopName(last.arrivalStop)}"
        val lines = buildList {
            driveBefore?.let { add("🚗 Drive to ${stopName(it)}") }
            addAll(boardings)
            add(arrival)
            driveAfter?.let { add("🚗 Drive on from ${stopName(it)}") }
        }
        return lines.joinToString("\n")
    }

    private fun transfersText(transfers: Int): String = when (transfers) {
        0 -> "Direct"
        1 -> "1 transfer"
        else -> "$transfers transfers"
    }

    private fun leaveWithin(departure: Instant, now: Instant?): String? {
        if (now == null) return null
        val minutes = Duration.between(now, departure).toMinutes()
        return if (minutes in 0..MAX_LEAVE_WITHIN_MINUTES) "leave within $minutes min" else null
    }

    /** Past this, a countdown is noise rather than a prompt to move. */
    private const val MAX_LEAVE_WITHIN_MINUTES = 60L

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
