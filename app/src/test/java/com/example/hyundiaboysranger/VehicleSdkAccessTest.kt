package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class VehicleSdkAccessTest {
    private class FakeVehicle(private val calls: MutableList<String>) {
        init { calls.add("create") }
        fun query() { calls.add("query") }
        fun release() { calls.add("release") }
    }

    @Test fun disabledPolicyNeverCreatesQueriesOrReleases() {
        val calls = mutableListOf<String>()
        val vehicle = VehicleSdkAccess.create { FakeVehicle(calls) }
        vehicle?.query(); vehicle?.release()
        assertFalse(VehicleSdkAccess.enabled)
        assertNull(vehicle)
        assertTrue(calls.isEmpty())
    }

    @Test fun blockedResultCannotAdvanceWiperOrConfirmDoors() {
        var progress = PracticeProgress.WAIT_OFF
        repeat(5) { progress = WiperLesson.observe(progress, SignalResult.SafetyBlocked.valueOrNull()) }
        assertEquals(PracticeProgress.WAIT_OFF, progress)
        val results = ChecklistDoor.entries.associateWith { SignalResult.SafetyBlocked }
        assertFalse(allChecklistDoorsClosed(results))
    }
}
