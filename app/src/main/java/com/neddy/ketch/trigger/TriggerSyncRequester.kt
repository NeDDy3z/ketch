package com.neddy.ketch.trigger

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Entry point for requesting a trigger re-sync after watcher changes.
 * Keeps WorkManager (and the Context it needs) out of the ViewModels.
 */
class TriggerSyncRequester(private val context: Context) {

    fun requestSync() {
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<TriggerSyncWorker>().build())
    }
}
