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
    fun `body carries departure arrival and transfer count on one line`() {
        assertEquals(
            "16:00 Praha hl.n. (R41) 🚆 → arrives 17:00 · 1 transfer",
            ConnectionFormatter.notificationText(withTransfer, zone),
        )
    }

    @Test
    fun `a direct connection says so instead of counting transfers`() {
        assertEquals(
            "16:00 Praha hl.n. (S1) 🚆 → arrives 16:45 · Direct",
            ConnectionFormatter.notificationText(direct, zone),
        )
    }

    @Test
    fun `an imminent departure appends a leave within countdown`() {
        assertEquals(
            "16:00 Praha hl.n. (S1) 🚆 → arrives 16:45 · Direct · leave within 4 min",
            ConnectionFormatter.notificationText(
                direct,
                zone,
                now = Instant.parse("2026-07-14T13:56:00Z"),
            ),
        )
    }

    @Test
    fun `a distant departure gets no countdown`() {
        assertEquals(
            "16:00 Praha hl.n. (S1) 🚆 → arrives 16:45 · Direct",
            ConnectionFormatter.notificationText(
                direct,
                zone,
                now = Instant.parse("2026-07-14T10:00:00Z"),
            ),
        )
    }

    @Test
    fun `big text lists every boarding and the arrival on separate lines`() {
        assertEquals(
            "16:00 Praha hl.n. (R41) 🚆\n" +
                "16:30 Cesky Brod (660) 🚌\n" +
                "17:00 Kostelec n.C. lesy",
            ConnectionFormatter.notificationBigText(withTransfer, zone),
        )
    }

    @Test
    fun `big text for a direct connection is boarding then arrival`() {
        assertEquals(
            "16:00 Praha hl.n. (S1) 🚆\n16:45 Kolin",
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
            "16:00 Praha hl.n. (S7) 🚆 → arrives 17:00 · 1 transfer",
            ConnectionFormatter.notificationText(connection, zone),
        )
        assertEquals(
            "16:00 Praha hl.n. (S7) 🚆\n" +
                "16:20 I.P. Pavlova (381) 🚌\n" +
                "17:00 K.n.Č.l, nám.",
            ConnectionFormatter.notificationBigText(connection, zone),
        )
    }

    @Test
    fun `a driven first leg is a line above the boardings`() {
        assertEquals(
            "🚗 Drive to Cesky Brod\n" +
                "16:00 Praha hl.n. (R41) 🚆\n" +
                "16:30 Cesky Brod (660) 🚌\n" +
                "17:00 Kostelec n.C. lesy",
            ConnectionFormatter.notificationBigText(
                withTransfer,
                zone,
                driveBefore = "Cesky Brod",
            ),
        )
    }

    @Test
    fun `a driven last leg is a line below the arrival and flagged in the body`() {
        assertEquals(
            "16:00 Praha hl.n. (R41) 🚆\n" +
                "16:30 Cesky Brod (660) 🚌\n" +
                "17:00 Kostelec n.C. lesy\n" +
                "🚗 Drive on from Cesky Brod",
            ConnectionFormatter.notificationBigText(
                withTransfer,
                zone,
                driveAfter = "Cesky Brod",
            ),
        )
        assertEquals(
            "16:00 Praha hl.n. (R41) 🚆 → arrives 17:00 · 1 transfer · then drive",
            ConnectionFormatter.notificationText(withTransfer, zone, driveAfter = true),
        )
    }
}
