package com.neddy.ketch.domain.model

/**
 * A public transport stop or station resolved from the places API.
 */
data class StopPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)
