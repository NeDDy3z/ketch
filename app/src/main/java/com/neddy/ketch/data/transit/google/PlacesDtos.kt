package com.neddy.ketch.data.transit.google

import kotlinx.serialization.Serializable

@Serializable
data class SearchTextRequest(
    val textQuery: String,
    val includedType: String = "transit_station",
    val maxResultCount: Int = 10,
)

@Serializable
data class SearchTextResponse(
    val places: List<PlaceDto> = emptyList(),
)

@Serializable
data class SearchNearbyRequest(
    val includedTypes: List<String> = listOf("transit_station"),
    val maxResultCount: Int = 1,
    val rankPreference: String = "DISTANCE",
    val locationRestriction: LocationRestrictionDto,
)

@Serializable
data class LocationRestrictionDto(
    val circle: CircleDto,
)

@Serializable
data class CircleDto(
    val center: LatLngDto,
    val radius: Double,
)

@Serializable
data class PlaceDto(
    val displayName: LocalizedTextDto? = null,
    val formattedAddress: String? = null,
    val location: LatLngDto? = null,
)

@Serializable
data class LocalizedTextDto(
    val text: String? = null,
)
