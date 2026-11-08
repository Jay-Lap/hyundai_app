package com.example.hyundiaboysranger

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assume.assumeTrue

/** Read-only: no vehicle control and no settings or review writes. */
class WiperDeviceReadTest {
    @Test fun readsCurrentSdkWiperState() = runBlocking {
        assumeTrue("Vehicle SDK access is safety-blocked", VehicleSdkAccess.enabled)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val reader = WiperReader(instrumentation.targetContext)
        try {
            val result = reader.read()
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "\nWiper SDK observation: $result\n") })
            assertTrue("Wiper SDK state not observed: $result", result is SignalResult.Value)
        } finally { reader.close() }
    }
}
