package com.neddy.ketch.trigger

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neddy.ketch.appContainer

/**
 * Re-registers geofences for all watchers. Runs after boot and after any
 * watcher change.
 */
class TriggerSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val watchers = container.watcherRepository.getWatchers()
        container.geofenceManager.sync(watchers)
        return Result.success()
    }
}
