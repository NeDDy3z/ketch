package com.neddy.ketch.domain.model

enum class TriggerType {
    /** Fires when the device leaves the trigger location (geofence exit). */
    LOCATION_EXIT,

    /** Fires at the start of the active time window on active days. */
    TIME,
}
