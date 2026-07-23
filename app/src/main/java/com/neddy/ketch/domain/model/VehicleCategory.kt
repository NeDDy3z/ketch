package com.neddy.ketch.domain.model

/**
 * Coarse grouping of the many provider vehicle type strings into the handful
 * of categories the user cares about when expressing a connection preference
 * ("I would rather take the train"). [OTHER] catches funiculars, cable cars
 * and anything unrecognised so it never silently maps to a wrong category.
 */
enum class VehicleCategory(val label: String) {
    BUS("Bus"),
    TRAIN("Train"),
    TRAM("Tram"),
    SUBWAY("Metro"),
    FERRY("Ferry"),
    OTHER("Other"),
    ;

    companion object {
        /** Categories a user can pick as a preference, in display order. */
        val selectable: List<VehicleCategory> = listOf(TRAIN, BUS, TRAM, SUBWAY, FERRY)

        fun fromType(vehicleType: String): VehicleCategory = when (vehicleType.uppercase()) {
            "HEAVY_RAIL", "COMMUTER_TRAIN", "HIGH_SPEED_TRAIN", "LONG_DISTANCE_TRAIN",
            "RAIL", "METRO_RAIL", "MONORAIL",
            -> TRAIN
            "SUBWAY" -> SUBWAY
            "TRAM", "LIGHT_RAIL" -> TRAM
            "FERRY" -> FERRY
            "BUS", "INTERCITY_BUS", "TROLLEYBUS", "SHARE_TAXI" -> BUS
            else -> OTHER
        }
    }
}
