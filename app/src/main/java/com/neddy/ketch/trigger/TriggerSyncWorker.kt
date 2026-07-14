package com.neddy.ketch.trigger

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neddy.ketch.appContainer

/**
 * Re-registers geofences and time triggers for all watchers. Runs after boot
 * and after any watcher change.
 */
class TriggerSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val watchers = container.watcherRepository.getWatchers()
        container.geofenceManager.sync(watchers)
        container.timeTriggerScheduler.sync(watchers)
        return Result.success()
    }
}
