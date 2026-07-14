package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.TransitLeg
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionFormatterTest {

    private val zone = ZoneId.of("Europe/Prague")

    private fun leg(
        line: String,
        from: String,
        departure: String,
        to: String,
        arrival: String,
    ) = TransitLeg(
        lineCode = line,
        vehicleType = "BUS",
        departureStop = from,
        departureTime = Instant.parse(departure),
        arrivalStop = to,
        arrivalTime = Instant.parse(arrival),
    )

    @Test
    fun `formats connection with transfer`() {
        val connection = TransitConnection(
            legs = listOf(
                leg(
                    "R41",
                    "Praha hl.n.",
                    "2026-07-14T14:00:00Z",
                    "Cesky Brod",
                    "2026-07-14T14:28:00Z",
                ),
                leg(
                    "660",
                    "Cesky Brod",
                    "2026-07-14T14:30:00Z",
                    "Kostelec n.C. lesy",
                    "2026-07-14T15:00:00Z",
                ),
            ),
        )

        assertEquals(
            "Praha hl.n. (R41) 16:00 - Cesky Brod (660) 16:30 - Kostelec n.C. lesy 17:00",
            ConnectionFormatter.format(connection, zone),
        )
    }

    @Test
    fun `formats direct connection`() {
        val connection = TransitConnection(
            legs = listOf(
                leg(
                    "S1",
                    "Praha hl.n.",
                    "2026-07-14T14:00:00Z",
                    "Kolin",
                    "2026-07-14T14:45:00Z",
                ),
            ),
        )

        assertEquals(
            "Praha hl.n. (S1) 16:00 - Kolin 16:45",
            ConnectionFormatter.format(connection, zone),
        )
    }
}
