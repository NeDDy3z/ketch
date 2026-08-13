package com.neddy.ketch.domain

import com.neddy.ketch.domain.model.TransitConnection
import java.time.Duration
import java.time.Instant

/**
 * Routing providers assume a slow, conservative walking pace, so a connection
 * that is comfortably catchable gets ruled out for someone who walks briskly.
 * The user sets how much of the calculated walk to shave off; everything here
 * works from that percentage.
 */
object WalkAdjustment {

    /** Never shift a lookup by more than this, whatever the walk. */
    val MAX_SHIFT: Duration = Duration.ofMinutes(15)

    /** The walk as actually walked, with [percent] taken off. */
    fun reduced(walk: Duration, percent: Int): Duration {
        val clamped = percent.coerceIn(0, MAX_PERCENT)
        if (clamped == 0 || walk.isNegative || walk.isZero) return walk
        return Duration.ofSeconds(walk.seconds * (100L - clamped) / 100L)
    }

    /** How much earlier the reduced walk reaches the first stop. */
    fun saving(walk: Duration, percent: Int): Duration {
        val saved = walk.minus(reduced(walk, percent))
        return if (saved > MAX_SHIFT) MAX_SHIFT else saved
    }

    /**
     * True when the user can still board [connection] leaving at [now]: the
     * reduced walk to the first stop has to end before the vehicle departs.
     */
    fun isReachable(connection: TransitConnection, now: Instant, percent: Int): Boolean {
        val arriveAtStop = now.plus(reduced(connection.accessWalk, percent))
        return !arriveAtStop.isAfter(connection.departureTime)
    }

    const val MAX_PERCENT = 50
    const val DEFAULT_PERCENT = 10
}
