package com.example.hyundiaboysranger

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.resume

class CallbackGateTest {
    private class FakeSdk {
        val callbacks = mutableListOf<(SignalResult<Boolean>) -> Unit>()
        suspend fun read(): SignalResult<Boolean> = suspendCancellableCoroutine { continuation ->
            val gate = CallbackGate()
            continuation.invokeOnCancellation { gate.cancel() }
            callbacks += { result -> if (gate.finish()) continuation.resume(result) }
        }
    }

    @Test fun successFailureAndDuplicateCallbacksAreOneShot() = runBlocking {
        val fake = FakeSdk()
        val success = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { fake.read() }
        fake.callbacks[0](SignalResult.Value(true))
        fake.callbacks[0](SignalResult.Error)
        assertEquals(SignalResult.Value(true), success.await())
        val failure = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { fake.read() }
        fake.callbacks[1](SignalResult.PermissionDenied)
        assertEquals(SignalResult.PermissionDenied, failure.await())
    }

    @Test fun timeoutCancellationLateCallbackAndRetryStayIndependent() = runBlocking {
        val fake = FakeSdk()
        assertNull(withTimeoutOrNull(20) { fake.read() })
        val cancelled = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { fake.read() }
        cancelled.cancelAndJoin()
        val retry = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { fake.read() }
        fake.callbacks[0](SignalResult.Value(true))
        fake.callbacks[1](SignalResult.Value(true))
        assertFalse(retry.isCompleted)
        fake.callbacks[2](SignalResult.Value(false))
        assertEquals(SignalResult.Value(false), retry.await())
        assertTrue(cancelled.isCancelled)
    }

    @Test fun lateNavigationEventsAreReportedWithoutClaimingRequestCompletion() {
        NavigationObservation.entries.forEach {
            assertTrue(uncorrelatedNavigationEvent(it).contains("이 요청의 결과인지 확인할 수 없어요"))
        }
    }
}
