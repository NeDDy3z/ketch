package com.neddy.ketch.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/**
 * Fired by the geofencing client when the device leaves a trigger location.
 * The actual lookup runs in [ConnectionLookupWorker] to stay off the main
 * thread and survive process death.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) return

        val watcherIds = event.triggeringGeofences
            .orEmpty()
            .mapNotNull { GeofenceManager.watcherId(it.requestId) }

        val workManager = WorkManager.getInstance(context)
        watcherIds.forEach { watcherId ->
            val request = OneTimeWorkRequestBuilder<ConnectionLookupWorker>()
                .setInputData(workDataOf(ConnectionLookupWorker.KEY_WATCHER_ID to watcherId))
                .build()
            workManager.enqueue(request)
        }
    }
}
