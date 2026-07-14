package com.neddy.ketch.data.transit.google

import com.neddy.ketch.data.settings.SettingsRepository
import com.neddy.ketch.data.transit.TransitRepository
import com.neddy.ketch.domain.model.PlaceSuggestion
import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.TransitLeg
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

    override suspend fun findConnections(
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
        val legs = legs
            .flatMap { it.steps }
            .filter { it.travelMode == "TRANSIT" }
            .mapNotNull { it.toLeg() }
        if (legs.isEmpty()) return null
        return TransitConnection(legs)
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

    companion object {
        private const val NEAREST_STOP_RADIUS_METERS = 500.0
        private const val MAX_ADDRESS_SUGGESTIONS = 3
    }
}
