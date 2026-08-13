package com.neddy.ketch.domain

/** What to do about an update prompt, decided before any network call. */
enum class UpdatePromptDecision { STAY_QUIET, PROMPT_FROM_CACHE, CHECK }

/**
 * When the app is allowed to raise the update prompt.
 *
 * The rule that matters: a launch always gets an answer. A check that found
 * nothing must not silence the next launch, or a release published in between
 * goes unnoticed for as long as the throttle lasts. Repeat checks are paced
 * only within one launch.
 */
object UpdatePolicy {

    /** Floor against a check loop, applied even to a fresh launch. */
    const val MIN_RECHECK_MS = 2 * 60 * 1000L

    /** How long a single launch coasts on its first check. */
    const val RECHECK_MS = 3 * 60 * 60 * 1000L

    fun decide(
        checksEnabled: Boolean,
        snoozedUntil: Long,
        lastCheckAt: Long,
        checkedThisLaunch: Boolean,
        knownNewerRelease: Boolean,
        now: Long,
    ): UpdatePromptDecision {
        if (!checksEnabled) return UpdatePromptDecision.STAY_QUIET
        if (now < snoozedUntil) return UpdatePromptDecision.STAY_QUIET
        // A release already heard of needs no network to prompt about.
        if (knownNewerRelease) return UpdatePromptDecision.PROMPT_FROM_CACHE
        val sinceLastCheck = now - lastCheckAt
        if (sinceLastCheck < MIN_RECHECK_MS) return UpdatePromptDecision.STAY_QUIET
        if (checkedThisLaunch && sinceLastCheck < RECHECK_MS) {
            return UpdatePromptDecision.STAY_QUIET
        }
        return UpdatePromptDecision.CHECK
    }
}
