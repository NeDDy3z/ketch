package com.neddy.ketch.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `a later release is newer`() {
        assertTrue(AppVersion.isNewer("v2.5", "2.4"))
    }

    @Test
    fun `the same release is not newer`() {
        assertFalse(AppVersion.isNewer("v2.4", "2.4"))
    }

    @Test
    fun `an older release is not newer`() {
        assertFalse(AppVersion.isNewer("v2.3", "2.4"))
    }

    @Test
    fun `components are compared as numbers not text`() {
        assertTrue(AppVersion.isNewer("v2.10", "2.9"))
    }

    @Test
    fun `a missing component counts as zero`() {
        assertTrue(AppVersion.isNewer("v2.4.1", "2.4"))
        assertFalse(AppVersion.isNewer("v2.4", "2.4.0"))
    }

    @Test
    fun `suffixes and prefixes are stripped`() {
        assertEquals(listOf(2, 4), AppVersion.parse(" v2.4-beta "))
    }

    @Test
    fun `an unparsable tag is never newer`() {
        assertFalse(AppVersion.isNewer("nightly", "2.4"))
    }
}
