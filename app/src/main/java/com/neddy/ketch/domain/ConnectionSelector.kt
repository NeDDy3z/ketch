package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.TransitConnection
import java.time.Duration

/**
 * Picks the best connection out of the alternatives returned by the routing
 * API. The best connection is the one arriving earliest at the destination
 * while respecting the optional watcher constraints.
 */
object ConnectionSelector {

    fun selectBest(
        connections: List<TransitConnection>,
        maxTransfers: Int? = null,
        maxTravelMinutes: Int? = null,
    ): TransitConnection? {
        return connections
            .asSequence()
            .filter { maxTransfers == null || it.transfers <= maxTransfers }
            .filter {
                maxTravelMinutes == null ||
                    it.travelDuration <= Duration.ofMinutes(maxTravelMinutes.toLong())
            }
            .sortedWith(compareBy({ it.arrivalTime }, { it.transfers }))
            .firstOrNull()
    }
}
