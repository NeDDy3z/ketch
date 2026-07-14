package com.neddy.ketch.domain.model

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A watcher describes one commute the user wants to be notified about,
 * for example "leaving home" or "leaving work". It fires when the device
 * leaves the trigger location; the route starts at the current position.
 */
data class Watcher(
    val id: Long = 0L,
    val name: String,
    /** Key of the icon shown for this watcher, see the UI icon catalog. */
    val icon: String = DEFAULT_ICON,
    val destination: StopPlace,
    /** Center of the leave geofence. */
    val triggerLatitude: Double,
    val triggerLongitude: Double,
    val triggerRadiusMeters: Int,
    val activeDays: Set<DayOfWeek>,
    /** Minutes from midnight, inclusive start of the active window. */
    val windowStartMinutes: Int,
    /** Minutes from midnight, inclusive end of the active window. */
    val windowEndMinutes: Int,
    val notificationsEnabled: Boolean = true,
    val maxTransfers: Int? = null,
    val maxTravelMinutes: Int? = null,
    val enabled: Boolean = true,
    /** Epoch millis of the last fired notification, used for cooldown. */
    val lastTriggeredAt: Long? = null,
) {

    val windowStart: LocalTime get() = LocalTime.ofSecondOfDay(windowStartMinutes * 60L)
    val windowEnd: LocalTime get() = LocalTime.ofSecondOfDay(windowEndMinutes * 60L)

    /**
     * True when [dateTime] falls on an active day inside the active time window.
     */
    fun isActiveAt(dateTime: LocalDateTime): Boolean {
        if (dateTime.dayOfWeek !in activeDays) return false
        val minutes = dateTime.hour * 60 + dateTime.minute
        return minutes in windowStartMinutes..windowEndMinutes
    }

    companion object {
        const val DEFAULT_ICON = "train"
    }
}
