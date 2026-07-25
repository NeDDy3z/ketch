package com.neddy.ketch.maps

import com.neddy.ketch.domain.model.StopPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitDirectionsTest {

    private val destination = StopPlace(
        name = "Praha hl.n.",
        latitude = 50.0831,
        longitude = 14.4356,
    )

    @Test
    fun `url routes to the destination coordinates by public transport`() {
        assertEquals(
            "https://www.google.com/maps?daddr=50.0831,14.4356&dirflg=r",
            TransitDirections.url(destination),
        )
    }

    @Test
    fun `url leaves the origin out so maps starts from the current position`() {
        assertTrue(!TransitDirections.url(destination).contains("saddr"))
    }
}
