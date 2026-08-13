package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.TransitLeg
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkAdjustmentTest {

    private fun connection(departure: String, walkMinutes: Long): TransitConnection =
        TransitConnection(
            legs = listOf(
                TransitLeg(
                    lineCode = "L",
                    vehicleType = "BUS",
                    departureStop = "A",
                    departureTime = Instant.parse(departure),
                    arrivalStop = "B",
                    arrivalTime = Instant.parse(departure).plus(Duration.ofMinutes(20)),
                ),
            ),
            accessWalk = Duration.ofMinutes(walkMinutes),
        )

    @Test
    fun `takes the percentage off the walk`() {
        assertEquals(
            Duration.ofMinutes(9),
            WalkAdjustment.reduced(Duration.ofMinutes(10), 10),
        )
    }

    @Test
    fun `zero percent leaves the walk alone`() {
        assertEquals(
            Duration.ofMinutes(10),
            WalkAdjustment.reduced(Duration.ofMinutes(10), 0),
        )
    }

    @Test
    fun `the saving is capped`() {
        // Two hours of walking at 50% would shift a lookup by an hour.
        assertEquals(
            WalkAdjustment.MAX_SHIFT,
            WalkAdjustment.saving(Duration.ofHours(2), 50),
        )
    }

    @Test
    fun `a departure inside the reduced walk is reachable`() {
        val now = Instant.parse("2026-07-14T08:00:00Z")
        // 20 min of walking, 10% off, so the stop is reached at 08:18.
        val connection = connection("2026-07-14T08:19:00Z", walkMinutes = 20)
        assertTrue(WalkAdjustment.isReachable(connection, now, 10))
    }

    @Test
    fun `a departure before the reduced walk ends is out of reach`() {
        val now = Instant.parse("2026-07-14T08:00:00Z")
        val connection = connection("2026-07-14T08:17:00Z", walkMinutes = 20)
        assertFalse(WalkAdjustment.isReachable(connection, now, 10))
    }
}
