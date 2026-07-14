package com.neddy.ketch.ui.components

import com.neddy.ketch.data.transit.google.MissingApiKeyException
import java.io.IOException
import retrofit2.HttpException

/**
 * Maps low level exceptions from lookups and searches to a short message a
 * user can act on, instead of raw HTTP or serialization noise.
 */
fun userMessageFor(error: Throwable): String = when (error) {
    is MissingApiKeyException ->
        "No API key configured. Add your Google Maps Platform key in Settings."
    is HttpException -> when (error.code()) {
        400 -> "The transit service rejected the request. Try a different stop."
        401, 403 ->
            "The API key was rejected. Check that the key is valid and that the " +
                "Routes API and Places API (New) are enabled."
        429 -> "Too many requests right now. Try again in a moment."
        in 500..599 -> "The transit service is having problems. Try again later."
        else -> "Lookup failed (HTTP ${error.code()}). Try again."
    }
    is IOException -> "No connection. Check your internet and try again."
    else -> "Something went wrong. Try again."
}
