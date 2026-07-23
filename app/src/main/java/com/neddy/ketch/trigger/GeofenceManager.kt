package com.neddy.ketch.trigger

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.neddy.ketch.data.location.LocationProvider
import com.neddy.ketch.domain.model.Watcher
import kotlinx.coroutines.tasks.await

/**
 * Keeps the registered geofences in sync with the enabled location triggered
 * watchers. Each watcher owns one EXIT geofence identified by "watcher_{id}".
 */
class GeofenceManager(private val context: Context) {

    private val client = LocationServices.getGeofencingClient(context)
    private val locationProvider = LocationProvider(context)

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    /**
     * Replaces all registered geofences with the ones derived from [watchers].
     * Requires background location permission, otherwise it is a no-op.
     */
    @SuppressLint("MissingPermission")
    suspend fun sync(watchers: List<Watcher>) {
        if (!locationProvider.hasForegroundPermission() ||
            !locationProvider.hasBackgroundPermission()
        ) {
            return
        }

        runCatching { client.removeGeofences(pendingIntent).await() }

        val geofences = watchers
            .filter { it.enabled }
            .map { watcher ->
                Geofence.Builder()
                    .setRequestId(requestId(watcher.id))
                    .setCircularRegion(
                        watcher.triggerLatitude,
                        watcher.triggerLongitude,
                        watcher.triggerRadiusMeters.toFloat(),
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    // Track ENTER as well as EXIT so the platform reliably
                    // establishes the inside/outside state; without a prior
                    // inside state a lone EXIT often never fires.
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or
                            Geofence.GEOFENCE_TRANSITION_EXIT,
                    )
                    // Deliver transitions as soon as they are detected instead
                    // of letting the OS batch them, which delayed or dropped
                    // some departures.
                    .setNotificationResponsiveness(0)
                    .build()
            }
        if (geofences.isEmpty()) return

        val request = GeofencingRequest.Builder()
            // If we register while the device is already at the trigger
            // location, seed the inside state right away so the next real exit
            // is detected.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()
        runCatching { client.addGeofences(request, pendingIntent).await() }
    }

    companion object {
        private const val REQUEST_ID_PREFIX = "watcher_"

        fun requestId(watcherId: Long): String = "$REQUEST_ID_PREFIX$watcherId"

        fun watcherId(requestId: String): Long? =
            requestId.removePrefix(REQUEST_ID_PREFIX).toLongOrNull()
    }
}
