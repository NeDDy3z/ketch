package com.neddy.ketch.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerConfirmationTest {

    @Test
    fun `no fix trusts the geofence and fires`() {
        assertTrue(TriggerConfirmation.exitConfirmed(distanceMeters = null, radiusMeters = 150))
    }

    @Test
    fun `confidently inside with an accurate fix is a spurious exit`() {
        // 20 m from center with 10 m accuracy: 30 m outer edge, radius 150,
        // hysteresis 50 -> threshold 100. Whole circle inside -> drop.
        assertFalse(
            TriggerConfirmation.exitConfirmed(
                distanceMeters = 20.0,
                radiusMeters = 150,
                accuracyMeters = 10.0,
            ),
        )
    }

    @Test
    fun `inside distance but coarse accuracy still fires`() {
        // 90 m from center, radius 150, hysteresis 50 -> threshold 100. With a
        // 40 m accuracy the outer edge is 130 m, not confidently inside, so a
        // real departure is not dropped.
        assertTrue(
            TriggerConfirmation.exitConfirmed(
                distanceMeters = 90.0,
                radiusMeters = 150,
                accuracyMeters = 40.0,
            ),
        )
    }

    @Test
    fun `beyond the radius is a real departure`() {
        assertTrue(
            TriggerConfirmation.exitConfirmed(
                distanceMeters = 300.0,
                radiusMeters = 150,
                accuracyMeters = 20.0,
            ),
        )
    }

    @Test
    fun `unknown accuracy only drops an obviously inside fix`() {
        // No accuracy -> margin 0. 20 m is under the 100 m threshold: drop.
        assertFalse(TriggerConfirmation.exitConfirmed(distanceMeters = 20.0, radiusMeters = 150))
        // 120 m is past the threshold: fire.
        assertTrue(TriggerConfirmation.exitConfirmed(distanceMeters = 120.0, radiusMeters = 150))
    }
}
