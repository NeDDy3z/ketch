package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.Watcher
import java.time.DayOfWeek
import java.time.ZonedDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatcherTest {

    private fun watcher(lastTriggeredAt: Long?) = Watcher(
        name = "Test",
        destination = StopPlace("B", 50.1, 14.1),
        triggerLatitude = 50.0,
        triggerLongitude = 14.0,
        triggerRadiusMeters = 150,
        activeDays = DayOfWeek.entries.toSet(),
        windowStartMinutes = 7 * 60,
        windowEndMinutes = 9 * 60,
        lastTriggeredAt = lastTriggeredAt,
    )

    private fun at(text: String): ZonedDateTime = ZonedDateTime.parse(text)

    @Test
    fun `never fired means the gate is open`() {
        val now = at("2026-07-14T08:00:00+02:00[Europe/Prague]")
        assertFalse(watcher(null).hasFiredInCurrentWindow(now))
    }

    @Test
    fun `fired inside the open window closes the gate`() {
        val now = at("2026-07-14T08:00:00+02:00[Europe/Prague]")
        val firedAt = at("2026-07-14T07:30:00+02:00[Europe/Prague]")
        assertTrue(
            watcher(firedAt.toInstant().toEpochMilli()).hasFiredInCurrentWindow(now),
        )
    }

    @Test
    fun `fired yesterday leaves the gate open today`() {
        val now = at("2026-07-14T07:05:00+02:00[Europe/Prague]")
        val firedAt = at("2026-07-13T07:30:00+02:00[Europe/Prague]")
        assertFalse(
            watcher(firedAt.toInstant().toEpochMilli()).hasFiredInCurrentWindow(now),
        )
    }

    @Test
    fun `gate reopens the next day without needing 24 hours`() {
        // Fired late in yesterday's window at 8:55, next window opens at
        // 7:00, only about 22 hours later.
        val firedAt = at("2026-07-13T08:55:00+02:00[Europe/Prague]")
        val now = at("2026-07-14T07:00:00+02:00[Europe/Prague]")
        assertFalse(
            watcher(firedAt.toInstant().toEpochMilli()).hasFiredInCurrentWindow(now),
        )
    }
}
