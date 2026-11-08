package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Exploratory model only: the real SDK does not expose the fake's callback-return event. */
class VehicleReleaseContractModelTest {
    private class FakeSdk(private val limit: Int = 2) {
        private val outstanding = mutableSetOf<Int>()
        private var nextId = 0
        private var closeRequested = false
        var released = false; private set
        var uiActive = true; private set
        val uiValues = mutableListOf<Int>()

        @Synchronized fun start(): Int? {
            if (closeRequested || released || outstanding.size >= limit) return null
            return (++nextId).also { outstanding.add(it) }
        }
        @Synchronized fun timeout() { uiActive = false }
        @Synchronized fun close() { uiActive = false; closeRequested = true; maybeRelease() }
        @Synchronized fun deliver(id: Int, value: Int) {
            if (id in outstanding && uiActive) uiValues.add(value)
        }
        // This event is provided by the test harness after a fake callback returns.
        // It is not an API claim about the installed SDK.
        @Synchronized fun callbackReturned(id: Int) {
            outstanding.remove(id)
            maybeRelease()
        }
        private fun maybeRelease() { if (closeRequested && outstanding.isEmpty()) released = true }
    }

    @Test fun uiTimeoutAndExitDoNotMeanSdkCompletion() {
        val sdk = FakeSdk()
        val first = sdk.start()!!
        sdk.timeout(); sdk.close(); sdk.deliver(first, 7)
        assertTrue(sdk.uiValues.isEmpty()); assertFalse(sdk.released); assertNull(sdk.start())
        sdk.callbackReturned(first)
        assertTrue(sdk.released)
    }

    @Test fun successFailureAndDuplicateCallbacksRequireAllExplicitReturns() {
        val sdk = FakeSdk()
        val first = sdk.start()!!; val second = sdk.start()!!
        assertNull(sdk.start())
        sdk.deliver(first, 1); sdk.callbackReturned(first); sdk.callbackReturned(first)
        sdk.close(); assertFalse(sdk.released)
        sdk.deliver(second, -1); sdk.callbackReturned(second)
        assertTrue(sdk.released); assertEquals(listOf(1), sdk.uiValues)
    }

    @Test fun noCallbackIsBoundedAndCannotBeReleased() {
        val sdk = FakeSdk(limit = 1)
        sdk.start(); sdk.timeout()
        repeat(20) { assertNull(sdk.start()) }
        sdk.close(); assertFalse(sdk.released)
    }

    @Test fun concurrentCloseAndCompletionNeverReleaseBeforeBothReturn() {
        repeat(100) {
            val sdk = FakeSdk(); val first = sdk.start()!!; val second = sdk.start()!!
            val pool = Executors.newFixedThreadPool(2); val gate = CountDownLatch(1)
            try {
                val close = pool.submit { gate.await(); sdk.close() }
                val callback = pool.submit { gate.await(); sdk.callbackReturned(first) }
                gate.countDown(); close.get(5, TimeUnit.SECONDS); callback.get(5, TimeUnit.SECONDS)
                assertFalse(sdk.released)
                sdk.callbackReturned(second); assertTrue(sdk.released)
            } finally { pool.shutdownNow() }
        }
    }
}
