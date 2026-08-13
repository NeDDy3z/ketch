package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.Watcher
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class OriginSelectorTest {

    private val parkAndRide = StopPlace("Depo Hostivar", 50.07, 14.55)

    private fun watcher(carStart: StopPlace?) = Watcher(
        name = "Leaving home",
        destination = StopPlace("Praha hl.n.", 50.08, 14.43),
        carStart = carStart,
        triggerLatitude = 50.0,
        triggerLongitude = 14.0,
        triggerRadiusMeters = 150,
        activeDays = DayOfWeek.entries.toSet(),
        windowStartMinutes = 7 * 60,
        windowEndMinutes = 9 * 60,
    )

    @Test
    fun `a fast leave starts at the car start`() {
        val origin = OriginSelector.select(
            watcher = watcher(parkAndRide),
            latitude = 50.001,
            longitude = 14.001,
            speedKmh = 32.0,
            carSpeedThresholdKmh = 15,
        )
        assertEquals(parkAndRide, origin)
    }

    @Test
    fun `a slow leave starts where the device is`() {
        val origin = OriginSelector.select(
            watcher = watcher(parkAndRide),
            latitude = 50.001,
            longitude = 14.001,
            speedKmh = 4.0,
            carSpeedThresholdKmh = 15,
        )
        assertEquals(50.001, origin.latitude, 0.0)
        assertEquals(14.001, origin.longitude, 0.0)
    }

    @Test
    fun `an unknown speed never picks the car start`() {
        val origin = OriginSelector.select(
            watcher = watcher(parkAndRide),
            latitude = 50.001,
            longitude = 14.001,
            speedKmh = null,
            carSpeedThresholdKmh = 15,
        )
        assertEquals(OriginSelector.CURRENT_LOCATION, origin.name)
    }

    @Test
    fun `without a car start a fast leave still starts where the device is`() {
        val origin = OriginSelector.select(
            watcher = watcher(null),
            latitude = 50.001,
            longitude = 14.001,
            speedKmh = 90.0,
            carSpeedThresholdKmh = 15,
        )
        assertEquals(OriginSelector.CURRENT_LOCATION, origin.name)
    }

    @Test
    fun `no fix falls back to the trigger location`() {
        val origin = OriginSelector.select(
            watcher = watcher(parkAndRide),
            latitude = null,
            longitude = null,
            speedKmh = null,
            carSpeedThresholdKmh = 15,
        )
        assertEquals(50.0, origin.latitude, 0.0)
        assertEquals(14.0, origin.longitude, 0.0)
    }
}
