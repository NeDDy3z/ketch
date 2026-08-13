package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.CarLeg
import com.neddy.ketch.domain.model.ParkedCar
import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.Watcher
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The commute these cases are drawn from: Kostelec nad Černými lesy to Prague,
 * driving the first stretch to Český Brod and taking the train on — except on
 * the days the car stays at home and the bus does that stretch instead.
 */
class JourneyPlannerTest {

    private val home = StopPlace("Kostelec n.Č.L.", 49.99, 14.86)
    private val brod = StopPlace("Český Brod", 50.07, 14.86)
    private val prague = StopPlace("Praha, Jana Masaryka", 50.07, 14.44)

    private val morning = 1_800_000_000_000L
    private val evening = morning + 10 * 60 * 60 * 1000L

    private fun watcher(
        destination: StopPlace,
        carLeg: CarLeg,
        carStop: StopPlace? = brod,
    ) = Watcher(
        name = "Commute",
        destination = destination,
        carStop = carStop,
        carLeg = carLeg,
        triggerLatitude = home.latitude,
        triggerLongitude = home.longitude,
        triggerRadiusMeters = 150,
        activeDays = DayOfWeek.entries.toSet(),
        windowStartMinutes = 7 * 60,
        windowEndMinutes = 9 * 60,
    )

    private val outbound = watcher(prague, CarLeg.TO_STOP)
    private val homebound = watcher(home, CarLeg.FROM_STOP)

    @Test
    fun `driving out starts the journey at the swap stop and parks the car there`() {
        val plan = JourneyPlanner.plan(
            watcher = outbound,
            latitude = home.latitude,
            longitude = home.longitude,
            speedKmh = 42.0,
            carSpeedThresholdKmh = 15,
            parkedCar = null,
            now = morning,
        )
        assertEquals(brod, plan.origin)
        assertEquals(prague, plan.destination)
        assertEquals(brod, plan.driveBefore)
        assertEquals(brod, plan.parksCarAt)
    }

    @Test
    fun `walking out keeps the whole journey on public transport`() {
        val plan = JourneyPlanner.plan(
            watcher = outbound,
            latitude = home.latitude,
            longitude = home.longitude,
            speedKmh = 4.0,
            carSpeedThresholdKmh = 15,
            parkedCar = null,
            now = morning,
        )
        assertEquals(JourneyPlanner.CURRENT_LOCATION, plan.origin.name)
        assertEquals(prague, plan.destination)
        assertNull(plan.driveBefore)
        assertNull(plan.parksCarAt)
    }

    @Test
    fun `coming home to a waiting car ends the journey at the car`() {
        val plan = JourneyPlanner.plan(
            watcher = homebound,
            latitude = prague.latitude,
            longitude = prague.longitude,
            speedKmh = null,
            carSpeedThresholdKmh = 15,
            parkedCar = ParkedCar(brod, morning),
            now = evening,
        )
        assertEquals(brod, plan.destination)
        assertEquals(brod, plan.driveAfter)
        assertEquals(JourneyPlanner.CURRENT_LOCATION, plan.origin.name)
    }

    @Test
    fun `coming home without the car runs all the way to the door`() {
        val plan = JourneyPlanner.plan(
            watcher = homebound,
            latitude = prague.latitude,
            longitude = prague.longitude,
            speedKmh = null,
            carSpeedThresholdKmh = 15,
            parkedCar = null,
            now = evening,
        )
        assertEquals(home, plan.destination)
        assertNull(plan.driveAfter)
    }

    @Test
    fun `a car left yesterday is not assumed to be waiting`() {
        val plan = JourneyPlanner.plan(
            watcher = homebound,
            latitude = prague.latitude,
            longitude = prague.longitude,
            speedKmh = null,
            carSpeedThresholdKmh = 15,
            parkedCar = ParkedCar(brod, morning - ParkedCar.TTL_MS),
            now = morning,
        )
        assertEquals(home, plan.destination)
        assertNull(plan.driveAfter)
    }

    @Test
    fun `a declared car day drives out even without a speed reading`() {
        // The switch on the details page parks the car up front, which is how a
        // drive the trigger failed to notice still shapes the journey.
        val plan = JourneyPlanner.plan(
            watcher = outbound,
            latitude = home.latitude,
            longitude = home.longitude,
            speedKmh = null,
            carSpeedThresholdKmh = 15,
            parkedCar = ParkedCar(brod, morning),
            now = morning,
        )
        assertEquals(brod, plan.origin)
        assertEquals(brod, plan.driveBefore)
        // Already recorded, so its timestamp is left alone.
        assertNull(plan.parksCarAt)
    }

    @Test
    fun `the drive home goes to where the car actually is`() {
        val elsewhere = StopPlace("Úvaly", 50.07, 14.73)
        val plan = JourneyPlanner.plan(
            watcher = homebound,
            latitude = prague.latitude,
            longitude = prague.longitude,
            speedKmh = null,
            carSpeedThresholdKmh = 15,
            parkedCar = ParkedCar(elsewhere, morning),
            now = evening,
        )
        assertEquals(elsewhere, plan.destination)
    }

    @Test
    fun `no car leg ignores a parked car entirely`() {
        val plan = JourneyPlanner.plan(
            watcher = watcher(prague, CarLeg.NONE),
            latitude = home.latitude,
            longitude = home.longitude,
            speedKmh = 60.0,
            carSpeedThresholdKmh = 15,
            parkedCar = ParkedCar(brod, morning),
            now = morning,
        )
        assertEquals(JourneyPlanner.CURRENT_LOCATION, plan.origin.name)
        assertEquals(prague, plan.destination)
    }

    @Test
    fun `a car leg without a stop falls back to the plain journey`() {
        val plan = JourneyPlanner.plan(
            watcher = watcher(prague, CarLeg.TO_STOP, carStop = null),
            latitude = home.latitude,
            longitude = home.longitude,
            speedKmh = 60.0,
            carSpeedThresholdKmh = 15,
            parkedCar = null,
            now = morning,
        )
        assertEquals(JourneyPlanner.CURRENT_LOCATION, plan.origin.name)
        assertEquals(prague, plan.destination)
    }

    @Test
    fun `no fix falls back to the trigger location`() {
        val plan = JourneyPlanner.plan(
            watcher = watcher(prague, CarLeg.NONE),
            latitude = null,
            longitude = null,
            speedKmh = null,
            carSpeedThresholdKmh = 15,
            parkedCar = null,
            now = morning,
        )
        assertEquals(home.latitude, plan.origin.latitude, 0.0)
        assertEquals(home.longitude, plan.origin.longitude, 0.0)
    }
}
