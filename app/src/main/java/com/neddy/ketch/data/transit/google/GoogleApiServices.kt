package com.neddy.ketch.data.transit.google

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Google Routes API v2, transit routing.
 * https://developers.google.com/maps/documentation/routes
 */
interface RoutesApiService {

    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String =
            "routes.legs.steps.travelMode,routes.legs.steps.transitDetails",
        @Body request: ComputeRoutesRequest,
    ): ComputeRoutesResponse
}

/**
 * Google Places API (New), text search restricted to transit stations.
 * https://developers.google.com/maps/documentation/places/web-service
 */
interface PlacesApiService {

    @POST("v1/places:searchText")
    suspend fun searchText(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String =
            "places.displayName,places.formattedAddress,places.location",
        @Body request: SearchTextRequest,
    ): SearchTextResponse
}
