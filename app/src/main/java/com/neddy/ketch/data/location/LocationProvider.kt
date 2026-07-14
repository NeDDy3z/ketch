package com.neddy.ketch.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class LocationProvider(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasForegroundPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    fun hasBackgroundPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Fast location for UI flows: returns the last known fix immediately and
     * only waits briefly for a fresh one when none is cached. A cold start
     * fresh fix can take many seconds, which is too slow for the home screen.
     */
    suspend fun quickLocation(timeoutMillis: Long = 3_000): Location? {
        if (!hasForegroundPermission()) return null
        return runCatching {
            client.lastLocation.await()
                ?: withTimeoutOrNull(timeoutMillis) {
                    client.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        CancellationTokenSource().token,
                    ).await()
                }
        }.getOrNull()
    }

    /**
     * Returns the current device location or null when permission is missing
     * or the location cannot be resolved. [precise] requests a high accuracy
     * fix, used when pinpointing on the map.
     */
    suspend fun currentLocation(precise: Boolean = false): Location? {
        if (!hasForegroundPermission()) return null
        val priority = if (precise) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        return runCatching {
            client.getCurrentLocation(
                priority,
                CancellationTokenSource().token,
            ).await() ?: client.lastLocation.await()
        }.getOrNull()
    }
}
