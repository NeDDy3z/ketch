package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.VehicleCategory
import java.time.Duration

/**
 * Picks the best connection out of the alternatives returned by the routing
 * API. Without a preference the best connection is the one arriving earliest
 * (ties broken by fewer transfers) while respecting the optional watcher
 * constraints.
 *
 * When a [preferredVehicle] is given, a connection that uses that vehicle
 * category is preferred over the plain fastest one, but only while it does
 * not arrive more than [maxTravelDeltaMinutes] later than the fastest. Past
 * that gap the faster (less preferred) connection wins so the user is never
 * sent far out of their way for the sake of the preference. The comparison is
 * on arrival time, not raw travel duration, because "fastest" is the earliest
 * arrival and the user cares about how much later they would get there.
 */
object ConnectionSelector {

    private val byArrival = compareBy<TransitConnection>({ it.arrivalTime }, { it.transfers })

    fun selectBest(
        connections: List<TransitConnection>,
        maxTransfers: Int? = null,
        maxTravelMinutes: Int? = null,
        preferredVehicle: VehicleCategory? = null,
        maxTravelDeltaMinutes: Int? = null,
    ): TransitConnection? {
        val eligible = connections.filter { connection ->
            (maxTransfers == null || connection.transfers <= maxTransfers) &&
                (
                    maxTravelMinutes == null ||
                        connection.travelDuration <= Duration.ofMinutes(maxTravelMinutes.toLong())
                    )
        }
        if (eligible.isEmpty()) return null

        val fastest = eligible.minWith(byArrival)
        if (preferredVehicle == null) return fastest

        val preferred = eligible
            .filter { it.usesCategory(preferredVehicle) }
            .minWithOrNull(byArrival)
            ?: return fastest

        if (maxTravelDeltaMinutes != null) {
            // How much later the preferred connection arrives than the fastest.
            val extraArrival = Duration.between(fastest.arrivalTime, preferred.arrivalTime)
            if (extraArrival > Duration.ofMinutes(maxTravelDeltaMinutes.toLong())) return fastest
        }
        return preferred
    }
}
