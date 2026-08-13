package com.neddy.ketch.data.transit.google

import com.neddy.ketch.data.settings.SettingsRepository
import com.neddy.ketch.data.transit.TransitRepository
import com.neddy.ketch.domain.WalkAdjustment
import com.neddy.ketch.domain.model.PlaceSuggestion
import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.TransitLeg
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

class MissingApiKeyException : IllegalStateException(
    "No Google Maps Platform API key configured. Add one in Settings or in local.properties.",
)

class GoogleTransitRepository(
    private val routesApi: RoutesApiService,
    private val placesApi: PlacesApiService,
    private val settingsRepository: SettingsRepository,
) : TransitRepository {

    /**
     * Routes departing at or after [departureTime]. With the walk reduction
     * switched on this costs a second lookup: the provider plans the walk at
     * its own pace, so the only way to surface the connections a faster walker
     * can still make is to ask again from an earlier departure and then drop
     * whatever is out of reach even at the reduced pace.
     */
    override suspend fun findConnections(
        origin: StopPlace,
        destination: StopPlace,
        departureTime: Instant,
    ): List<TransitConnection> {
        val connections = computeRoutes(origin, destination, departureTime)
        val percent = settingsRepository.current().walkReductionPercent
        if (percent <= 0 || connections.isEmpty()) return connections

        val longestWalk = connections.maxOf { it.accessWalk }
        val shift = WalkAdjustment.saving(longestWalk, percent)
        if (shift < MIN_WALK_SHIFT) return connections

        val earlier = runCatching {
            computeRoutes(origin, destination, departureTime.minus(shift))
        }.getOrDefault(emptyList())
        val reachable = earlier.filter {
            WalkAdjustment.isReachable(it, departureTime, percent)
        }
        // The earlier pass can miss departures the first one found, so the two
        // are merged rather than swapped; anything from the first pass is
        // reachable by definition, the provider planned it from now.
        return (reachable + connections).distinctBy { it.legs }
    }

    private suspend fun computeRoutes(
        origin: StopPlace,
        destination: StopPlace,
        departureTime: Instant,
    ): List<TransitConnection> {
        val response = routesApi.computeRoutes(
            apiKey = apiKey(),
            request = ComputeRoutesRequest(
                origin = origin.toWaypoint(),
                destination = destination.toWaypoint(),
                departureTime = DateTimeFormatter.ISO_INSTANT.format(departureTime),
            ),
        )
        return response.routes.mapNotNull { it.toConnection() }
    }

    override suspend fun searchStops(query: String): List<StopPlace> {
        if (query.isBlank()) return emptyList()
        val response = placesApi.searchText(
            apiKey = apiKey(),
            request = SearchTextRequest(textQuery = query),
        )
        return response.places.mapNotNull { place ->
            val name = place.displayName?.text ?: return@mapNotNull null
            val location = place.location ?: return@mapNotNull null
            StopPlace(
                name = name,
                latitude = location.latitude,
                longitude = location.longitude,
            )
        }
    }

    override suspend fun searchAddresses(query: String): List<PlaceSuggestion> {
        if (query.isBlank()) return emptyList()
        val response = placesApi.searchText(
            apiKey = apiKey(),
            request = SearchTextRequest(
                textQuery = query,
                includedType = null,
                maxResultCount = MAX_ADDRESS_SUGGESTIONS,
            ),
        )
        return response.places.mapNotNull { place ->
            val name = place.displayName?.text ?: return@mapNotNull null
            val location = place.location ?: return@mapNotNull null
            PlaceSuggestion(
                name = name,
                address = place.formattedAddress,
                latitude = location.latitude,
                longitude = location.longitude,
            )
        }.take(MAX_ADDRESS_SUGGESTIONS)
    }

    override suspend fun nearestStop(latitude: Double, longitude: Double): StopPlace? {
        val response = placesApi.searchNearby(
            apiKey = apiKey(),
            request = SearchNearbyRequest(
                locationRestriction = LocationRestrictionDto(
                    circle = CircleDto(
                        center = LatLngDto(latitude, longitude),
                        radius = NEAREST_STOP_RADIUS_METERS,
                    ),
                ),
            ),
        )
        val place = response.places.firstOrNull() ?: return null
        val name = place.displayName?.text ?: return null
        val location = place.location ?: return null
        return StopPlace(
            name = name,
            latitude = location.latitude,
            longitude = location.longitude,
        )
    }

    private suspend fun apiKey(): String {
        val key = settingsRepository.current().apiKey
        if (key.isBlank()) throw MissingApiKeyException()
        return key
    }

    private fun StopPlace.toWaypoint() = WaypointDto(
        location = LocationDto(latLng = LatLngDto(latitude, longitude)),
    )

    private fun RouteDto.toConnection(): TransitConnection? {
        val steps = legs.flatMap { it.steps }
        val transitLegs = steps
            .filter { it.travelMode == "TRANSIT" }
            .mapNotNull { it.toLeg() }
        if (transitLegs.isEmpty()) return null
        // Everything walked before the first boarding, which is what stands
        // between leaving now and catching that vehicle.
        val accessWalk = steps
            .takeWhile { it.travelMode != "TRANSIT" }
            .mapNotNull { it.staticDuration?.let(::parseDuration) }
            .fold(Duration.ZERO, Duration::plus)
        return TransitConnection(legs = transitLegs, accessWalk = accessWalk)
    }

    private fun RouteStepDto.toLeg(): TransitLeg? {
        val details = transitDetails ?: return null
        val stops = details.stopDetails ?: return null
        val departureStop = stops.departureStop?.name ?: return null
        val arrivalStop = stops.arrivalStop?.name ?: return null
        val departureTime = stops.departureTime?.let(::parseInstant) ?: return null
        val arrivalTime = stops.arrivalTime?.let(::parseInstant) ?: return null
        val line = details.transitLine
        return TransitLeg(
            lineCode = line?.nameShort ?: line?.name ?: "?",
            vehicleType = line?.vehicle?.type ?: "UNKNOWN",
            departureStop = departureStop,
            departureTime = departureTime,
            arrivalStop = arrivalStop,
            arrivalTime = arrivalTime,
            headsign = details.headsign.orEmpty(),
        )
    }

    private fun parseInstant(value: String): Instant? =
        runCatching { Instant.parse(value) }.getOrNull()

    /** Protobuf duration, seconds with a trailing "s". */
    private fun parseDuration(value: String): Duration? =
        value.removeSuffix("s").toDoubleOrNull()
            ?.let { Duration.ofSeconds(it.toLong()) }

    companion object {
        private const val NEAREST_STOP_RADIUS_METERS = 500.0
        private const val MAX_ADDRESS_SUGGESTIONS = 3

        /** Below this the second lookup cannot buy an earlier departure. */
        private val MIN_WALK_SHIFT: Duration = Duration.ofMinutes(1)
    }
}
