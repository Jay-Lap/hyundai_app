package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

class FeatureGateTest {
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
}
