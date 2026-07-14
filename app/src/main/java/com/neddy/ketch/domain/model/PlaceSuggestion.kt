package com.neddy.ketch.domain.model

/**
 * An address or place suggestion from the search-as-you-type lookup in the
 * map picker.
 */
data class PlaceSuggestion(
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
)
