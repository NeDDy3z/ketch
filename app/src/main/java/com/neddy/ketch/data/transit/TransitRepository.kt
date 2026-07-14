package com.neddy.ketch.data.transit

import com.neddy.ketch.domain.model.PlaceSuggestion
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

    /**
     * Returns the transit stop closest to the coordinates, or null when no
     * stop exists within a reasonable walking distance.
     */
    suspend fun nearestStop(latitude: Double, longitude: Double): StopPlace?

    /**
     * Searches addresses and places matching [query] for the map picker,
     * returning at most a few suggestions.
     */
    suspend fun searchAddresses(query: String): List<PlaceSuggestion>
}
