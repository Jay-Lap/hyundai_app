package com.example.hyundiaboysranger

import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Cold flow: callers own lifecycle and stop collection before releasing the adapter. */
class VehicleStateHub(private val read: suspend () -> ParkingEvidence, private val intervalMs: Long = 750) {
    fun states(): Flow<ParkingEvidence> = flow {
        while (true) {
            val evidence = try {
                read()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ParkingEvidence()
            }
            emit(evidence)
            delay(intervalMs)
        }
    }
}
