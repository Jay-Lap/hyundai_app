package com.example.hyundiaboysranger

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VehicleSafetyStopTest {
    @Test fun allReaderEntryPointsReturnBlockedWithoutADeviceSignal() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse(VehicleSdkAccess.enabled)
        val wiper = WiperReader(context)
        val health = VehicleHealthReader(context)
        val door = VehicleDoorReader(context)
        try {
            assertEquals(SignalResult.SafetyBlocked, wiper.read())
            assertEquals(SignalResult.SafetyBlocked, health.read().beltResult)
            assertTrue(health.readChecklistDoors().values.all { it == SignalResult.SafetyBlocked })
            var message: String? = null
            door.read { message = it }
            assertEquals("차량 연동 안전 점검 중", message)
        } finally { wiper.close(); health.close(); door.close() }
    }
}
