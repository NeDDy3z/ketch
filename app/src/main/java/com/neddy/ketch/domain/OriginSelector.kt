package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.Watcher

/**
 * Where a lookup starts from.
 *
 * How fast the device left says which journey this is. Strolling out means the
 * nearest stop on foot, so the route starts wherever the device is. Pulling
 * away at speed means the car is out, and the walk to a local stop is not what
 * happens next: the route starts at the watcher's car start point, typically a
 * station the car gets parked at.
 */
object OriginSelector {

    const val CURRENT_LOCATION = "Current location"

    /**
     * [speedKmh] is the speed at the moment of leaving, null when unknown —
     * an unknown speed is treated as walking, so the car start never hijacks a
     * lookup on a guess.
     */
    fun select(
        watcher: Watcher,
        latitude: Double?,
        longitude: Double?,
        speedKmh: Double?,
        carSpeedThresholdKmh: Int,
    ): StopPlace {
        val carStart = watcher.carStart
        if (carStart != null && speedKmh != null && speedKmh >= carSpeedThresholdKmh) {
            return carStart
        }
        return StopPlace(
            name = CURRENT_LOCATION,
            latitude = latitude ?: watcher.triggerLatitude,
            longitude = longitude ?: watcher.triggerLongitude,
        )
    }

    /** True when [speedKmh] is fast enough to count as a car journey. */
    fun isDriving(speedKmh: Double?, carSpeedThresholdKmh: Int): Boolean =
        speedKmh != null && speedKmh >= carSpeedThresholdKmh
}
