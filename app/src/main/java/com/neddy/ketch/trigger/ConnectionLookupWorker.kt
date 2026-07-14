package com.neddy.ketch.trigger

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neddy.ketch.appContainer
import com.neddy.ketch.domain.ConnectionFormatter
import com.neddy.ketch.domain.ConnectionSelector
import com.neddy.ketch.domain.model.StopPlace
import java.time.Instant
import java.time.LocalDateTime

/**
 * Looks up the current fastest connection for one watcher and posts the
 * notification. Enqueued by the geofence receiver. The route starts at the
 * current device position, falling back to the trigger location.
 */
class ConnectionLookupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val watcherId = inputData.getLong(KEY_WATCHER_ID, -1L)
        if (watcherId < 0) return Result.failure()

        val watcher = container.watcherRepository.getWatcher(watcherId)
            ?: return Result.failure()

        if (!watcher.enabled || !watcher.notificationsEnabled) return Result.success()
        if (!watcher.isActiveAt(LocalDateTime.now())) return Result.success()

        val now = Instant.now()
        val last = watcher.lastTriggeredAt
        if (last != null && now.toEpochMilli() - last < COOLDOWN_MILLIS) {
            return Result.success()
        }

        val location = container.locationProvider.currentLocation()
        val origin = StopPlace(
            name = "Current location",
            latitude = location?.latitude ?: watcher.triggerLatitude,
            longitude = location?.longitude ?: watcher.triggerLongitude,
        )

        val connections = try {
            container.transitRepository.findConnections(
                origin = origin,
                destination = watcher.destination,
                departureTime = now,
            )
        } catch (e: Exception) {
            return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }

        val best = ConnectionSelector.selectBest(
            connections,
            maxTransfers = watcher.maxTransfers,
            maxTravelMinutes = watcher.maxTravelMinutes,
        ) ?: return Result.success()

        container.notificationHelper.notifyConnection(
            watcher,
            ConnectionFormatter.format(best),
        )
        container.watcherRepository.markTriggered(watcher.id, now.toEpochMilli())
        return Result.success()
    }

    companion object {
        const val KEY_WATCHER_ID = "watcher_id"
        private const val MAX_RETRIES = 3

        /** Suppress repeated notifications for the same watcher for 30 minutes. */
        const val COOLDOWN_MILLIS = 30L * 60L * 1000L
    }
}
