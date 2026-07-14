package com.neddy.ketch.trigger

import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.TriggerType
import com.neddy.ketch.domain.model.Watcher
import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeTriggerSchedulerTest {

    private fun watcher(days: Set<DayOfWeek>, startMinutes: Int) = Watcher(
        name = "Test",
        origin = StopPlace("A", 50.0, 14.0),
        destination = StopPlace("B", 50.1, 14.1),
        triggerType = TriggerType.TIME,
        triggerLatitude = 50.0,
        triggerLongitude = 14.0,
        triggerRadiusMeters = 150,
        activeDays = days,
        windowStartMinutes = startMinutes,
        windowEndMinutes = startMinutes + 120,
    )

    @Test
    fun `next occurrence is today when window is still ahead`() {
        // 2026-07-14 is a Tuesday
        val now = LocalDateTime.parse("2026-07-14T06:00:00")
        val next = TimeTriggerScheduler.nextOccurrence(
            watcher(setOf(DayOfWeek.TUESDAY), 7 * 60),
            now,
        )
        assertEquals(LocalDateTime.parse("2026-07-14T07:00:00"), next)
    }

    @Test
    fun `next occurrence skips to next active day when window passed`() {
        val now = LocalDateTime.parse("2026-07-14T08:00:00")
        val next = TimeTriggerScheduler.nextOccurrence(
            watcher(setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY), 7 * 60),
            now,
        )
        assertEquals(LocalDateTime.parse("2026-07-17T07:00:00"), next)
    }

    @Test
    fun `next occurrence wraps to next week`() {
        val now = LocalDateTime.parse("2026-07-14T08:00:00")
        val next = TimeTriggerScheduler.nextOccurrence(
            watcher(setOf(DayOfWeek.MONDAY), 7 * 60),
            now,
        )
        assertEquals(LocalDateTime.parse("2026-07-20T07:00:00"), next)
    }

    @Test
    fun `no occurrence without active days`() {
        val now = LocalDateTime.parse("2026-07-14T08:00:00")
        assertNull(TimeTriggerScheduler.nextOccurrence(watcher(emptySet(), 7 * 60), now))
    }
}
