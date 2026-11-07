package com.example.hyundiaboysranger

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Cold flow: callers own lifecycle and stop collection before releasing the adapter. */
class VehicleStateHub(private val read: suspend () -> ParkingEvidence, private val intervalMs: Long = 750) {
    fun states(): Flow<ParkingEvidence> = flow {
        while (true) {
            emit(read())
            delay(intervalMs)
        }
    }
}
