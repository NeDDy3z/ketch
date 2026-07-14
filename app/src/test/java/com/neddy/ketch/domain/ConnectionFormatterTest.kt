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
        vehicleType: String = "BUS",
    ) = TransitLeg(
        lineCode = line,
        vehicleType = vehicleType,
        departureStop = from,
        departureTime = Instant.parse(departure),
        arrivalStop = to,
        arrivalTime = Instant.parse(arrival),
    )

    private val withTransfer = TransitConnection(
        legs = listOf(
            leg(
                "R41",
                "Praha hl.n.",
                "2026-07-14T14:00:00Z",
                "Cesky Brod",
                "2026-07-14T14:28:00Z",
                vehicleType = "HEAVY_RAIL",
            ),
            leg(
                "660",
                "Cesky Brod",
                "2026-07-14T14:30:00Z",
                "Kostelec n.C. lesy",
                "2026-07-14T15:00:00Z",
                vehicleType = "BUS",
            ),
        ),
    )

    private val direct = TransitConnection(
        legs = listOf(
            leg(
                "S1",
                "Praha hl.n.",
                "2026-07-14T14:00:00Z",
                "Kolin",
                "2026-07-14T14:45:00Z",
                vehicleType = "COMMUTER_TRAIN",
            ),
        ),
    )

    @Test
    fun `title is time then stop then line then emoji`() {
        assertEquals(
            "16:00 Praha hl.n. (R41) 🚆",
            ConnectionFormatter.notificationTitle(withTransfer, zone),
        )
    }

    @Test
    fun `text continues after the first boarding on one line`() {
        assertEquals(
            "16:30 Cesky Brod (660) 🚌 - 17:00 Kostelec n.C. lesy",
            ConnectionFormatter.notificationText(withTransfer, zone),
        )
    }

    @Test
    fun `big text continues after the first boarding on separate lines`() {
        assertEquals(
            "16:30 Cesky Brod (660) 🚌\n17:00 Kostelec n.C. lesy",
            ConnectionFormatter.notificationBigText(withTransfer, zone),
        )
    }

    @Test
    fun `direct connection body is just the arrival`() {
        assertEquals(
            "16:00 Praha hl.n. (S1) 🚆",
            ConnectionFormatter.notificationTitle(direct, zone),
        )
        assertEquals(
            "16:45 Kolin",
            ConnectionFormatter.notificationBigText(direct, zone),
        )
    }

    @Test
    fun `verbose stop names are shortened`() {
        val connection = TransitConnection(
            legs = listOf(
                leg(
                    "S7",
                    "Hlavní nádraží",
                    "2026-07-14T14:00:00Z",
                    "Masarykovo nádraží",
                    "2026-07-14T14:10:00Z",
                    vehicleType = "COMMUTER_TRAIN",
                ),
                leg(
                    "381",
                    "I. P. Pavlova",
                    "2026-07-14T14:20:00Z",
                    "Kostelec n.Č.L.,Nám.",
                    "2026-07-14T15:00:00Z",
                    vehicleType = "BUS",
                ),
            ),
        )

        assertEquals(
            "16:00 Praha hl.n. (S7) 🚆",
            ConnectionFormatter.notificationTitle(connection, zone),
        )
        assertEquals(
            "16:20 I.P. Pavlova (381) 🚌\n17:00 K.n.Č.l, nám.",
            ConnectionFormatter.notificationBigText(connection, zone),
        )
    }
}
