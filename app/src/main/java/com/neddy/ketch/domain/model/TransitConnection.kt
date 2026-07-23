package com.neddy.ketch.domain.model

import java.time.Duration
import java.time.Instant

/**
 * One boarding of a public transport vehicle. Walking segments are never
 * represented, a connection consists of transit legs only.
 */
data class TransitLeg(
    /** Public code of the line, for example "R41", "660" or "A". */
    val lineCode: String,
    val vehicleType: String,
    val departureStop: String,
    val departureTime: Instant,
    val arrivalStop: String,
    val arrivalTime: Instant,
    val headsign: String = "",
) {
    val category: VehicleCategory get() = VehicleCategory.fromType(vehicleType)
}

/**
 * A complete connection from origin to destination composed of transit legs.
 */
data class TransitConnection(
    val legs: List<TransitLeg>,
) {
    init {
        require(legs.isNotEmpty()) { "A connection needs at least one transit leg" }
    }

    val departureTime: Instant get() = legs.first().departureTime
    val arrivalTime: Instant get() = legs.last().arrivalTime
    val transfers: Int get() = legs.size - 1
    val travelDuration: Duration get() = Duration.between(departureTime, arrivalTime)

    /** True when at least one boarding uses the given vehicle [category]. */
    fun usesCategory(category: VehicleCategory): Boolean =
        legs.any { it.category == category }
}
