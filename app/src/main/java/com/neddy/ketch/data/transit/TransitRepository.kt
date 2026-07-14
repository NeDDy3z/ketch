package com.neddy.ketch.data.transit

import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.TransitConnection
import java.time.Instant

/**
 * Abstraction over the transit data provider. The default implementation is
 * backed by the Google Routes and Places APIs, but the interface allows
 * swapping in a regional provider such as Golemio (PID) later.
 */
interface TransitRepository {

    /**
     * Returns transit connections from [origin] to [destination] departing at
     * or after [departureTime], including alternatives.
     */
    suspend fun findConnections(
        origin: StopPlace,
        destination: StopPlace,
        departureTime: Instant,
    ): List<TransitConnection>

    /**
     * Searches public transport stops matching the free text [query].
     */
    suspend fun searchStops(query: String): List<StopPlace>
}
