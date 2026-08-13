package com.neddy.ketch.domain.model

/**
 * Which stretch of a journey the car can cover, if the car is available at all.
 *
 * A commute is often part road, part rails: drive to the station, take the
 * train, and on the way back take the train to the station and drive home from
 * it. The stretch is a possibility, not a promise — some days the car stays put
 * and the whole journey is public transport, which is why the car has to be
 * known to be out before [FROM_STOP] can be used.
 */
enum class CarLeg(val label: String) {
    /** No part of this journey is driven. */
    NONE("No car"),

    /** From where you leave to the swap stop, then transit onwards. */
    TO_STOP("Drive to the stop"),

    /** Transit to the swap stop, then drive the rest. */
    FROM_STOP("Drive from the stop"),
    ;

    val usesCar: Boolean get() = this != NONE
}
