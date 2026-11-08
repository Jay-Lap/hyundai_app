package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class FeatureGateTest {
    @org.junit.Test fun capabilityDistinguishesAccessAndMissingArea() {
        org.junit.Assert.assertEquals(SignalResult.ReadNotAvailable, capabilityResult(false, false))
        org.junit.Assert.assertEquals(SignalResult.AreaNotReported, capabilityResult(true, false))
        org.junit.Assert.assertEquals(SignalResult.Value(Unit), capabilityResult(true, true))
        org.junit.Assert.assertNull(SignalResult.AreaNotReported.valueOrNull())
    }
    @Test fun allInteractiveFeaturesFailClosedWithoutFreshParkEvidence() {
        val valid = ParkingEvidence(true, 0f, true, 100)
        for (feature in AppFeature.entries) {
            assertTrue(FeatureGate.allowed(feature, valid, 101))
            assertFalse(FeatureGate.allowed(feature, valid.copy(park = false), 101))
            assertFalse(FeatureGate.allowed(feature, valid.copy(speed = 1f), 101))
            assertFalse(FeatureGate.allowed(feature, valid.copy(unrestricted = null), 101))
            assertFalse(FeatureGate.allowed(feature, valid, 2601))
        }
    }
    @Test fun securityExceptionIsNotConfusedWithGenericFailure() {
        assertEquals(SignalResult.PermissionDenied, signalFailure(SecurityException()))
        assertEquals(SignalResult.Error, signalFailure(IllegalStateException()))
        assertNull(SignalResult.Timeout.valueOrNull())
        assertEquals(true, SignalResult.Value(true).valueOrNull())
    }
    @Test fun hubEmitsFakeReadingsAndStopsWhenCollectionEnds() = kotlinx.coroutines.runBlocking {
        var count = 0
        val hub = VehicleStateHub({ count++; ParkingEvidence(park = count == 1) }, intervalMs = 1)
        val values = hub.states().take(2).toList()
        assertEquals(listOf(true, false), values.map { it.park })
        assertEquals(2, count)
    }
    @Test fun fakeDrivingLossAndReconnectFailClosedUntilFreshPark() = kotlinx.coroutines.runBlocking {
        var clock = 100L
        val samples = listOf(
            ParkingEvidence(true, 0f, true, 100),
            ParkingEvidence(false, 3f, true, 101),
            ParkingEvidence(), // service disconnected
            ParkingEvidence(true, 0f, true, 50), // old sample after reconnect
            ParkingEvidence(true, 0f, true, 3000),
        )
        var index = 0
        val hub = VehicleStateHub({ samples[index++] }, intervalMs = 1)
        val observed = mutableListOf<Boolean>()
        hub.states().take(samples.size).collect { sample ->
            clock = when (index) { 1 -> 100; 2 -> 101; 3 -> 102; 4 -> 3000; else -> 3000 }
            observed += FeatureGate.allowed(AppFeature.PRACTICE, sample, clock)
        }
        assertEquals(listOf(true, false, false, false, true), observed)
    }
    @Test fun stalledFakeReadExpiresAndCancellationStopsHub() = kotlinx.coroutines.runBlocking {
        val valid = ParkingEvidence(true, 0f, true, 100)
        val started = CompletableDeferred<Unit>()
        val never = CompletableDeferred<ParkingEvidence>()
        var reads = 0
        val hub = VehicleStateHub({
            reads++
            if (reads == 1) valid else { started.complete(Unit); never.await() }
        }, intervalMs = 1)
        val values = mutableListOf<ParkingEvidence>()
        val job = launch { hub.states().collect { values.add(it) } }
        started.await()
        assertTrue(FeatureGate.allowed(AppFeature.PRACTICE, values.single(), 2600))
        assertFalse(FeatureGate.allowed(AppFeature.PRACTICE, values.single(), 2601))
        job.cancel(); job.join(); yield()
        assertEquals(2, reads)
        assertEquals(1, values.size)
    }
}
