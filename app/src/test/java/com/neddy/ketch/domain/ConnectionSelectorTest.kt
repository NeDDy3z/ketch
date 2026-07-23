package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.TransitLeg
import com.neddy.ketch.domain.model.VehicleCategory
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

    private fun single(
        vehicleType: String,
        departure: String,
        arrival: String,
    ): TransitConnection = TransitConnection(
        legs = listOf(
            TransitLeg(
                lineCode = "L",
                vehicleType = vehicleType,
                departureStop = "A",
                departureTime = Instant.parse(departure),
                arrivalStop = "B",
                arrivalTime = Instant.parse(arrival),
            ),
        ),
    )

    // Train: 14:00 -> 15:00 (60 min). Bus: 14:00 -> 14:40 (40 min, fastest).
    private val trainSlow = single("HEAVY_RAIL", "2026-07-14T14:00:00Z", "2026-07-14T15:00:00Z")
    private val busFast = single("BUS", "2026-07-14T14:00:00Z", "2026-07-14T14:40:00Z")

    // Train departs later (14:20) and arrives 15:05: 45 min of travel but it
    // arrives 25 min after the bus. Duration delta vs bus is only 5 min.
    private val trainLateArrival =
        single("HEAVY_RAIL", "2026-07-14T14:20:00Z", "2026-07-14T15:05:00Z")

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

    @Test
    fun `prefers the preferred vehicle within the travel delta`() {
        val best = ConnectionSelector.selectBest(
            listOf(busFast, trainSlow),
            preferredVehicle = VehicleCategory.TRAIN,
            maxTravelDeltaMinutes = 30,
        )
        assertEquals(trainSlow, best)
    }

    @Test
    fun `falls back to fastest when the preferred exceeds the travel delta`() {
        val best = ConnectionSelector.selectBest(
            listOf(busFast, trainSlow),
            preferredVehicle = VehicleCategory.TRAIN,
            maxTravelDeltaMinutes = 10,
        )
        assertEquals(busFast, best)
    }

    @Test
    fun `prefers the preferred vehicle when no delta is set`() {
        val best = ConnectionSelector.selectBest(
            listOf(busFast, trainSlow),
            preferredVehicle = VehicleCategory.TRAIN,
        )
        assertEquals(trainSlow, best)
    }

    @Test
    fun `uses fastest when no connection matches the preference`() {
        val best = ConnectionSelector.selectBest(
            listOf(busFast, trainSlow),
            preferredVehicle = VehicleCategory.SUBWAY,
        )
        assertEquals(busFast, best)
    }

    @Test
    fun `preference delta is measured by arrival time not travel duration`() {
        // The train arrives 25 min after the bus, over the 10 min delta, even
        // though its travel duration is only 5 min longer. Arrival wins.
        val best = ConnectionSelector.selectBest(
            listOf(busFast, trainLateArrival),
            preferredVehicle = VehicleCategory.TRAIN,
            maxTravelDeltaMinutes = 10,
        )
        assertEquals(busFast, best)
    }

    @Test
    fun `preference still respects the hard limits`() {
        // Train is preferred but breaks the max travel minutes limit, so the
        // bus is the only eligible connection.
        val best = ConnectionSelector.selectBest(
            listOf(busFast, trainSlow),
            maxTravelMinutes = 50,
            preferredVehicle = VehicleCategory.TRAIN,
        )
        assertEquals(busFast, best)
    }
}
