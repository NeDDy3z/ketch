package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.TransitConnection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formats a connection into the compact single line used in notifications,
 * for example:
 *
 * "Praha hl.n. (R41) 16:00 - Cesky Brod (660) 16:30 - Kostelec n.C. lesy 17:00"
 *
 * Every boarding is rendered as "stop (line) departure" and the final stop is
 * rendered as "stop arrival". Walking segments are excluded by design.
 */
object ConnectionFormatter {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun format(connection: TransitConnection, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val boardings = connection.legs.joinToString(" - ") { leg ->
            "${leg.departureStop} (${leg.lineCode}) ${time(leg.departureTime, zoneId)}"
        }
        val last = connection.legs.last()
        return "$boardings - ${last.arrivalStop} ${time(last.arrivalTime, zoneId)}"
    }

    private fun time(instant: Instant, zoneId: ZoneId): String =
        timeFormatter.format(instant.atZone(zoneId))
}
