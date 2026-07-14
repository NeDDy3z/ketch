package com.neddy.ketch.trigger

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.neddy.ketch.domain.model.TriggerType
import com.neddy.ketch.domain.model.Watcher
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Schedules time based watchers. Each watcher gets a unique one-shot work
 * request at the next window start on an active day. The lookup worker
 * reschedules the next occurrence after it runs.
 */
class TimeTriggerScheduler(private val context: Context) {

    fun sync(watchers: List<Watcher>) {
        watchers.forEach { watcher ->
            if (watcher.enabled && watcher.triggerType == TriggerType.TIME) {
                schedule(watcher)
            } else {
                cancel(watcher.id)
            }
        }
    }

    fun schedule(watcher: Watcher, now: LocalDateTime = LocalDateTime.now()) {
        val next = nextOccurrence(watcher, now) ?: return
        val delay = Duration.between(now, next)
        val request = OneTimeWorkRequestBuilder<ConnectionLookupWorker>()
            .setInitialDelay(delay)
            .setInputData(
                workDataOf(
                    ConnectionLookupWorker.KEY_WATCHER_ID to watcher.id,
                    ConnectionLookupWorker.KEY_RESCHEDULE to true,
                ),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(watcher.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(watcherId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(watcherId))
    }

    companion object {
        private fun uniqueName(watcherId: Long) = "time_trigger_$watcherId"

        /**
         * The next date time at which the watcher window opens, strictly
         * after [now]. Returns null when no day is active.
         */
        fun nextOccurrence(watcher: Watcher, now: LocalDateTime): LocalDateTime? {
            if (watcher.activeDays.isEmpty()) return null
            val start = LocalTime.ofSecondOfDay(watcher.windowStartMinutes * 60L)
            for (offset in 0..7L) {
                val candidate = now.toLocalDate().plusDays(offset).atTime(start)
                if (candidate.dayOfWeek in watcher.activeDays && candidate.isAfter(now)) {
                    return candidate
                }
            }
            return null
        }
    }
}
