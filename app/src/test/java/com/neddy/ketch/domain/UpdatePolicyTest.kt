package com.neddy.ketch.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatePolicyTest {

    private val now = 1_800_000_000_000L

    private fun decide(
        checksEnabled: Boolean = true,
        snoozedUntil: Long = 0L,
        lastCheckAt: Long = 0L,
        checkedThisLaunch: Boolean = false,
        knownNewerRelease: Boolean = false,
    ) = UpdatePolicy.decide(
        checksEnabled = checksEnabled,
        snoozedUntil = snoozedUntil,
        lastCheckAt = lastCheckAt,
        checkedThisLaunch = checkedThisLaunch,
        knownNewerRelease = knownNewerRelease,
        now = now,
    )

    @Test
    fun `switched off stays quiet`() {
        assertEquals(UpdatePromptDecision.STAY_QUIET, decide(checksEnabled = false))
    }

    @Test
    fun `a live snooze stays quiet`() {
        assertEquals(
            UpdatePromptDecision.STAY_QUIET,
            decide(snoozedUntil = now + 1_000),
        )
    }

    @Test
    fun `an expired snooze asks again`() {
        assertEquals(UpdatePromptDecision.CHECK, decide(snoozedUntil = now - 1_000))
    }

    @Test
    fun `a release already known needs no network`() {
        assertEquals(
            UpdatePromptDecision.PROMPT_FROM_CACHE,
            decide(knownNewerRelease = true, lastCheckAt = now - 1_000),
        )
    }

    @Test
    fun `a new launch checks even right after the last check`() {
        // The bug this guards: a check that found nothing used to silence the
        // next launch for hours, so a release published in between went unseen.
        assertEquals(
            UpdatePromptDecision.CHECK,
            decide(lastCheckAt = now - 10 * 60 * 1000),
        )
    }

    @Test
    fun `the same launch does not re-check straight away`() {
        assertEquals(
            UpdatePromptDecision.STAY_QUIET,
            decide(lastCheckAt = now - 10 * 60 * 1000, checkedThisLaunch = true),
        )
    }

    @Test
    fun `the same launch checks again after a long wait`() {
        assertEquals(
            UpdatePromptDecision.CHECK,
            decide(
                lastCheckAt = now - UpdatePolicy.RECHECK_MS - 1,
                checkedThisLaunch = true,
            ),
        )
    }

    @Test
    fun `a relaunch inside the floor stays quiet`() {
        assertEquals(UpdatePromptDecision.STAY_QUIET, decide(lastCheckAt = now - 1_000))
    }
}
