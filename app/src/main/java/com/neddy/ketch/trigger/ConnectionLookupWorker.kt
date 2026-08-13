package com.neddy.ketch.trigger

import android.content.Context
import android.location.Location
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neddy.ketch.appContainer
import com.neddy.ketch.domain.ConnectionFormatter
import com.neddy.ketch.domain.ConnectionSelector
import com.neddy.ketch.domain.OriginSelector
import com.neddy.ketch.domain.TriggerConfirmation
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime

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

        // Fire at most once per time window. The gate resets at the next
        // window start instead of requiring a calendar day to pass.
        if (watcher.hasFiredInCurrentWindow(ZonedDateTime.now())) return Result.success()

        val now = Instant.now()

        val location = container.locationProvider.currentLocation()

        // Drop spurious exit events: only when a fresh, accurate fix places us
        // confidently inside the trigger radius. A coarse fix never suppresses.
        val distance = location?.let {
            val results = FloatArray(1)
            Location.distanceBetween(
                it.latitude,
                it.longitude,
                watcher.triggerLatitude,
                watcher.triggerLongitude,
                results,
            )
            results[0].toDouble()
        }
        val accuracy = location?.takeIf { it.hasAccuracy() }?.accuracy?.toDouble()
        if (!TriggerConfirmation.exitConfirmed(
                distanceMeters = distance,
                radiusMeters = watcher.triggerRadiusMeters,
                accuracyMeters = accuracy,
            )
        ) {
            return Result.success()
        }

        // The fix behind the trigger is the one moment the app knows how the
        // user actually left, so this is where the car start earns its keep.
        val speedKmh = location?.takeIf { it.hasSpeed() }?.let { it.speed * 3.6 }
        val origin = OriginSelector.select(
            watcher = watcher,
            latitude = location?.latitude,
            longitude = location?.longitude,
            speedKmh = speedKmh,
            carSpeedThresholdKmh = container.settingsRepository.current().carSpeedThresholdKmh,
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
            preferredVehicle = watcher.preferredVehicle,
            maxTravelDeltaMinutes = watcher.maxTravelDeltaMinutes,
        ) ?: return Result.success()

        // Ketch owns two strings: the watcher's name, and one line carrying the
        // whole decision. Everything else is Android's notification chrome.
        container.notificationHelper.notifyConnection(
            watcher,
            watcher.name,
            ConnectionFormatter.notificationText(best, now = now),
            ConnectionFormatter.notificationBigText(best),
        )
        container.watcherRepository.markTriggered(watcher.id, now.toEpochMilli())
        return Result.success()
    }

    companion object {
        const val KEY_WATCHER_ID = "watcher_id"
        private const val MAX_RETRIES = 3
    }
}
