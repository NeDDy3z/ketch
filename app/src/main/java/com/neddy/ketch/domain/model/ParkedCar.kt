package com.neddy.ketch.domain.model

/**
 * Where the car is waiting, once it has been driven out to a swap stop.
 *
 * This is the state that makes "the car can cover that leg" a fact rather than
 * a guess: the drive home from the station is only on if the car was driven
 * there in the first place. The record lapses after [TTL_MS] so it never
 * survives into the next morning, when the car might well stay at home.
 */
data class ParkedCar(
    val place: StopPlace,
    /** Epoch millis the car was left there. */
    val parkedAt: Long,
) {

    fun isOutAt(now: Long): Boolean = now - parkedAt in 0 until TTL_MS

    companion object {
        /** Long enough for a working day, short enough to expire overnight. */
        const val TTL_MS = 14 * 60 * 60 * 1000L
    }
}
