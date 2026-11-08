package com.example.hyundiaboysranger

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VehicleStateHubRecoveryTest {
    @Test fun readExceptionProducesUnknownThenRetries() = runBlocking {
        var calls = 0
        val parked = ParkingEvidence(true, 0f, true, 100)
        val hub = VehicleStateHub({
            calls++
            if (calls == 2) throw IllegalStateException("connection lost") else parked
        }, intervalMs = 1)
        val values = hub.states().take(3).toList()
        assertEquals(listOf(parked, ParkingEvidence(), parked), values)
        assertFalse(FeatureGate.allowed(AppFeature.PRACTICE, values[1], 100))
        assertEquals(3, calls)
    }

    @Test fun cancellationIsNotConvertedToUnknownOrRetried() = runBlocking {
        var calls = 0
        val hub = VehicleStateHub({ calls++; throw CancellationException("stop") }, intervalMs = 1)
        try {
            hub.states().toList()
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(1, calls)
        }
    }
}
