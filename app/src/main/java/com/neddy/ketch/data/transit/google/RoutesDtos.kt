package com.neddy.ketch.data.transit.google

import kotlinx.serialization.Serializable

@Serializable
data class LatLngDto(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class LocationDto(
    val latLng: LatLngDto,
)

@Serializable
data class WaypointDto(
    val location: LocationDto,
)

@Serializable
data class ComputeRoutesRequest(
    val origin: WaypointDto,
    val destination: WaypointDto,
    val travelMode: String = "TRANSIT",
    /** RFC 3339 timestamp. */
    val departureTime: String,
    val computeAlternativeRoutes: Boolean = true,
)

@Serializable
data class ComputeRoutesResponse(
    val routes: List<RouteDto> = emptyList(),
)

@Serializable
data class RouteDto(
    val legs: List<RouteLegDto> = emptyList(),
)

@Serializable
data class RouteLegDto(
    val steps: List<RouteStepDto> = emptyList(),
)

@Serializable
data class RouteStepDto(
    val travelMode: String? = null,
    val transitDetails: TransitDetailsDto? = null,
)

@Serializable
data class TransitDetailsDto(
    val stopDetails: StopDetailsDto? = null,
    val headsign: String? = null,
    val transitLine: TransitLineDto? = null,
)

@Serializable
data class StopDetailsDto(
    val arrivalStop: TransitStopDto? = null,
    val arrivalTime: String? = null,
    val departureStop: TransitStopDto? = null,
    val departureTime: String? = null,
)

@Serializable
data class TransitStopDto(
    val name: String? = null,
    val location: LocationDto? = null,
)

@Serializable
data class TransitLineDto(
    val name: String? = null,
    val nameShort: String? = null,
    val vehicle: TransitVehicleDto? = null,
)

@Serializable
data class TransitVehicleDto(
    val type: String? = null,
)
