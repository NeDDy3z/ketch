package com.neddy.ketch.domain.model

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

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
    /**
     * Preferred vehicle category. When set, a connection using this category
     * is chosen over the fastest one, as long as it is not slower than the
     * fastest by more than [maxTravelDeltaMinutes]. Null means no preference.
     */
    val preferredVehicle: VehicleCategory? = null,
    /**
     * How many minutes longer the preferred connection may take compared to
     * the fastest one before the fastest is picked instead. Null means the
     * preference always wins when a matching connection exists.
     */
    val maxTravelDeltaMinutes: Int? = null,
    val enabled: Boolean = true,
    /** Sort position on the home screen, ascending. Lower shows first. */
    val sortOrder: Int = 0,
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

    /**
     * True when the watcher already fired inside the time window that is
     * open at [now]. The gate resets at the next window start, so a full
     * 24 hours never needs to pass between two windows.
     */
    fun hasFiredInCurrentWindow(now: ZonedDateTime): Boolean {
        val last = lastTriggeredAt ?: return false
        val windowOpenedAt = now.toLocalDate()
            .atTime(windowStart)
            .atZone(now.zone)
            .toInstant()
            .toEpochMilli()
        return last >= windowOpenedAt
    }

    companion object {
        const val DEFAULT_ICON = "train"
    }
}
