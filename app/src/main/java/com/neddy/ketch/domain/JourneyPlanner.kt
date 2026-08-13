package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.CarLeg
import com.neddy.ketch.domain.model.ParkedCar
import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.Watcher

/**
 * Works out which two points the transit lookup actually runs between, once the
 * drivable leg of the journey is taken into account.
 *
 * Outbound, the car covers the stretch from the door to the swap stop, so the
 * transit journey starts at that stop and the car is left there. Coming back,
 * the car is waiting at that stop, so the transit journey ends there and the
 * last stretch is driven. Neither happens unless the car is genuinely in play:
 * on a day the car stays at home, both journeys are plain public transport
 * between the real endpoints.
 */
object JourneyPlanner {

    const val CURRENT_LOCATION = "Current location"

    /**
     * The endpoints a lookup should use, plus what happens either side of it.
     */
    data class Plan(
        val origin: StopPlace,
        val destination: StopPlace,
        /** Driven before boarding, so the transit journey starts here. */
        val driveBefore: StopPlace? = null,
        /** Driven after arriving, so the transit journey ends here. */
        val driveAfter: StopPlace? = null,
        /** Set when this journey leaves the car somewhere, to be recorded. */
        val parksCarAt: StopPlace? = null,
    ) {
        val usesCar: Boolean get() = driveBefore != null || driveAfter != null
    }

    /**
     * [speedKmh] is how fast the device is moving as it leaves, null when
     * unknown — only the trigger knows this, and an unknown speed never counts
     * as driving. [parkedCar] is where the car was last left, if anywhere.
     */
    fun plan(
        watcher: Watcher,
        latitude: Double?,
        longitude: Double?,
        speedKmh: Double?,
        carSpeedThresholdKmh: Int,
        parkedCar: ParkedCar?,
        now: Long,
    ): Plan {
        val here = StopPlace(
            name = CURRENT_LOCATION,
            latitude = latitude ?: watcher.triggerLatitude,
            longitude = longitude ?: watcher.triggerLongitude,
        )
        val plain = Plan(origin = here, destination = watcher.destination)
        val stop = watcher.carStop ?: return plain
        val carIsOut = parkedCar?.isOutAt(now) == true

        return when (watcher.carLeg) {
            CarLeg.NONE -> plain

            // Driving out: either caught in the act by the speed of the leave,
            // or already declared by a car left at this stop earlier today.
            CarLeg.TO_STOP -> {
                val driving = isDriving(speedKmh, carSpeedThresholdKmh)
                if (!driving && !carIsOut) {
                    plain
                } else {
                    Plan(
                        origin = stop,
                        destination = watcher.destination,
                        driveBefore = stop,
                        // Only a fresh drive re-stamps where the car is; a
                        // standing record keeps its original time.
                        parksCarAt = stop.takeIf { driving },
                    )
                }
            }

            // Coming back: the drive home is only on when the car is actually
            // waiting, and it is waiting wherever it was left, not wherever
            // this watcher nominates.
            CarLeg.FROM_STOP -> {
                val waiting = parkedCar?.place?.takeIf { carIsOut } ?: return plain
                Plan(
                    origin = here,
                    destination = waiting,
                    driveAfter = waiting,
                )
            }
        }
    }

    /** True when [speedKmh] is fast enough to be a car rather than a walk. */
    fun isDriving(speedKmh: Double?, carSpeedThresholdKmh: Int): Boolean =
        speedKmh != null && speedKmh >= carSpeedThresholdKmh
}
