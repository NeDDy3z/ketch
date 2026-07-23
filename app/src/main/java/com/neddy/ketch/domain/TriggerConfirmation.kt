package com.neddy.ketch.domain

/**
 * Decides whether a geofence transition should actually fire a lookup.
 *
 * Platform geofencing is noisy: exit events sometimes arrive from GPS jitter
 * while the device is still sitting at home, and duplicate transitions are
 * common. This pure helper adds a distance based confirmation with hysteresis
 * so a spurious exit is dropped, while strongly biasing toward firing so a
 * real departure is never missed.
 */
object TriggerConfirmation {

    /** Default slack around the radius, in meters, to absorb GPS jitter. */
    const val DEFAULT_HYSTERESIS_METERS = 50.0

    /**
     * True when an EXIT transition should be honored.
     *
     * @param distanceMeters measured distance from the trigger center to the
     *   current device position, or null when no fresh fix is available.
     * @param radiusMeters the watcher trigger radius.
     * @param accuracyMeters the horizontal accuracy of the fix (its error
     *   radius), or null when unknown.
     *
     * The exit is honored unless we are *confidently* still inside: the whole
     * uncertainty circle of the fix (distance plus its accuracy) sits inside
     * the radius minus the hysteresis slack. A coarse or inaccurate fix
     * therefore never suppresses a real departure, and with no fix at all we
     * trust the geofence and fire. Only an obvious, well-measured false exit
     * from jitter while parked at the trigger is dropped.
     */
    fun exitConfirmed(
        distanceMeters: Double?,
        radiusMeters: Int,
        accuracyMeters: Double? = null,
        hysteresisMeters: Double = DEFAULT_HYSTERESIS_METERS,
    ): Boolean {
        if (distanceMeters == null) return true
        val errorMargin = accuracyMeters ?: 0.0
        val confidentlyInside = distanceMeters + errorMargin < radiusMeters - hysteresisMeters
        return !confidentlyInside
    }
}
