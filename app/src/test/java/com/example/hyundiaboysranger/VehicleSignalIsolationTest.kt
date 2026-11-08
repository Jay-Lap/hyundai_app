package com.example.hyundiaboysranger

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VehicleSignalIsolationTest {
    @Test fun sdkErrorsAreClassifiedWithoutGuessingTheirCause() {
        assertEquals(SignalResult.PermissionDenied, signalFailure(SecurityException()))
        assertEquals(SignalResult.Error, signalFailure(IllegalStateException()))
        assertEquals(SignalResult.Error, signalFailure(LinkageError()))
        assertEquals(SignalResult.Unsupported, SignalResult.Unsupported.flatMap { SignalResult.Value(true) })
    }

    @Test fun oneDomainFailureDoesNotReplaceAnotherAndCancellationPropagates() = runBlocking {
        val door = isolatedSignalRead<SignalResult<Boolean>>({ throw SecurityException() }) { it }
        val belt = isolatedSignalRead<SignalResult<Boolean>>({ SignalResult.Value(true) }) { it }
        assertEquals(SignalResult.PermissionDenied, door)
        assertEquals(SignalResult.Value(true), belt)
        try {
            isolatedSignalRead<SignalResult<Boolean>>({ throw CancellationException("cancel") }) { it }
            fail("cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    @Test fun healthScreenReducerClearsOldDataWhileLoadingAndStoresOnlyNewResult() {
        val old = VehicleHealthUiState(reading = VehicleHealth(), timestamp = "old", startedAt = 1)
        val loading = reduceVehicleHealth(old, VehicleHealthUiEvent.Started(20))
        assertTrue(loading.loading); assertNull(loading.reading); assertEquals(20L, loading.startedAt)
        val updated = reduceVehicleHealth(loading, VehicleHealthUiEvent.Finished(VehicleHealth(washerResult = SignalResult.Error), "new"))
        assertFalse(updated.loading); assertEquals("new", updated.timestamp); assertEquals(SignalResult.Error, updated.reading!!.washerResult)
    }
}
