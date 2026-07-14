package com.neddy.ketch.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neddy.ketch.appContainer
import com.neddy.ketch.domain.ConnectionFormatter
import com.neddy.ketch.domain.ConnectionSelector
import com.neddy.ketch.domain.model.StopPlace
import java.time.Instant

/**
 * Fetches the current fastest connection for every watcher shown by any
 * widget, caches the formatted lines, and re-renders the widgets.
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val container = context.appContainer
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(KetchWidget::class.java)
        if (glanceIds.isEmpty()) return Result.success()

        val watcherIds = glanceIds
            .flatMap { WidgetPrefs.selectedWatchers(context, manager.getAppWidgetId(it)) }
            .toSet()
        if (watcherIds.isEmpty()) {
            KetchWidget().updateAll(context)
            return Result.success()
        }

        val location = container.locationProvider.quickLocation()
        watcherIds.forEach { watcherId ->
            val watcher = container.watcherRepository.getWatcher(watcherId)
                ?: return@forEach
            val line = try {
                val origin = StopPlace(
                    name = "Current location",
                    latitude = location?.latitude ?: watcher.triggerLatitude,
                    longitude = location?.longitude ?: watcher.triggerLongitude,
                )
                val connections = container.transitRepository.findConnections(
                    origin = origin,
                    destination = watcher.destination,
                    departureTime = Instant.now(),
                )
                val best = ConnectionSelector.selectBest(
                    connections,
                    maxTransfers = watcher.maxTransfers,
                    maxTravelMinutes = watcher.maxTravelMinutes,
                )
                if (best != null) {
                    ConnectionFormatter.notificationTitle(best) + " - " +
                        ConnectionFormatter.notificationText(best)
                } else {
                    "No connection found"
                }
            } catch (e: Exception) {
                "Lookup failed"
            }
            WidgetPrefs.setConnectionLine(context, watcherId, line)
        }

        KetchWidget().updateAll(context)
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "widget_refresh",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
            )
        }
    }
}
