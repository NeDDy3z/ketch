package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.TransitLeg
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionSelectorTest {

    private fun connection(vararg times: Pair<String, String>): TransitConnection =
        TransitConnection(
            legs = times.mapIndexed { index, (dep, arr) ->
                TransitLeg(
                    lineCode = "L$index",
                    vehicleType = "BUS",
                    departureStop = "Stop$index",
                    departureTime = Instant.parse(dep),
                    arrivalStop = "Stop${index + 1}",
                    arrivalTime = Instant.parse(arr),
                )
            },
        )

    private val direct = connection(
        "2026-07-14T14:05:00Z" to "2026-07-14T15:10:00Z",
    )

    private val fastWithTransfer = connection(
        "2026-07-14T14:00:00Z" to "2026-07-14T14:28:00Z",
        "2026-07-14T14:30:00Z" to "2026-07-14T15:00:00Z",
    )

    @Test
    fun `selects earliest arrival`() {
        val best = ConnectionSelector.selectBest(listOf(direct, fastWithTransfer))
        assertEquals(fastWithTransfer, best)
    }

    @Test
    fun `respects max transfers`() {
        val best = ConnectionSelector.selectBest(
            listOf(direct, fastWithTransfer),
            maxTransfers = 0,
        )
        assertEquals(direct, best)
    }

    @Test
    fun `respects max travel minutes`() {
        val best = ConnectionSelector.selectBest(
            listOf(direct, fastWithTransfer),
            maxTravelMinutes = 61,
        )
        assertEquals(fastWithTransfer, best)
    }

    @Test
    fun `returns null when nothing matches`() {
        val best = ConnectionSelector.selectBest(
            listOf(direct, fastWithTransfer),
            maxTravelMinutes = 10,
        )
        assertNull(best)
    }
}
