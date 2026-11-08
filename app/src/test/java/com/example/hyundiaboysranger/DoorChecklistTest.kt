package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class DoorChecklistTest {
    private fun park(now: Long) = ParkingEvidence(true, 0f, true, now)
    private fun closed() = ChecklistDoor.entries.associateWith<ChecklistDoor, SignalResult<DoorPosition>> { SignalResult.Value(DoorPosition.CLOSED) }

    @Test fun exactlyFourSupportedClosedDoorsCanBeConfirmed() {
        val controller = DoorChecklistController()
        val token = controller.begin(park(100), 100)!!
        controller.complete(token, park(101), 101, 123456, closed())
        assertTrue(controller.state.confirmed)
        assertEquals(123456L, controller.state.snapshot!!.displayedTime)
        assertEquals(4, controller.state.snapshot!!.results.size)
    }

    @Test fun openMissingUnknownAndEveryFailureNeverConfirm() {
        val failures: List<SignalResult<DoorPosition>> = listOf(
            SignalResult.Value(DoorPosition.OPEN), SignalResult.Value(DoorPosition.UNKNOWN),
            SignalResult.NoSignal, SignalResult.ReadNotAvailable, SignalResult.AreaNotReported,
            SignalResult.PermissionDenied, SignalResult.Timeout, SignalResult.EmulatorUnavailable, SignalResult.Error,
            SignalResult.Unsupported, SignalResult.Restricted, SignalResult.ServiceUnavailable, SignalResult.SafetyBlocked,
        )
        for (failure in failures) for (door in ChecklistDoor.entries) {
            val controller = DoorChecklistController()
            val token = controller.begin(park(0), 0)!!
            controller.complete(token, park(1), 1, 2, closed() + (door to failure))
            assertFalse("$door $failure", controller.state.confirmed)
        }
        val controller = DoorChecklistController()
        val token = controller.begin(park(0), 0)!!
        controller.complete(token, park(1), 1, 2, closed() - ChecklistDoor.REAR_RIGHT)
        assertFalse(controller.state.confirmed)
    }

    @Test fun staleResultAndLostParkingClearOnlyAutomaticConfirmation() {
        val controller = DoorChecklistController()
        val token = controller.begin(park(100), 100)!!
        controller.complete(token, park(101), 101, 1000, closed())
        assertTrue(controller.state.confirmed)
        controller.refresh(park(15_100), 15_100)
        assertTrue(controller.state.confirmed)
        controller.refresh(park(15_101), 15_101)
        assertFalse(controller.state.confirmed)
        assertNotNull(controller.state.snapshot)
        val next = controller.begin(park(20_000), 20_000)!!
        controller.complete(next, park(20_001), 20_001, 2000, closed())
        controller.refresh(ParkingEvidence(), 20_002)
        assertFalse(controller.state.confirmed)
        assertNull(controller.begin(ParkingEvidence(), 20_003))
    }

    @Test fun lateOrCancelledResponsesCannotChangeNewerOrClosedScreen() {
        val controller = DoorChecklistController()
        val first = controller.begin(park(0), 0)!!
        controller.clear() // explicit uncheck invalidates even an in-flight result
        val second = controller.begin(park(10), 10)!!
        controller.complete(first, park(11), 11, 11, closed())
        assertTrue(controller.state.loading)
        assertFalse(controller.state.confirmed)
        controller.complete(second, park(12), 12, 12, closed())
        assertTrue(controller.state.confirmed)
        controller.clear(clearSnapshot = true) // reset / manual uncheck
        controller.complete(second, park(13), 13, 13, closed())
        assertEquals(DoorChecklistState(), controller.state)
        val third = controller.begin(park(14), 14)!!
        controller.close()
        controller.complete(third, park(15), 15, 15, closed())
        assertEquals(DoorChecklistState(), controller.state)
        assertNull(controller.begin(park(16), 16))
    }

    @Test fun staleAndGateLossDuringPendingReadRejectTheResult() {
        val controller = DoorChecklistController()
        val first = controller.begin(park(100), 100)!!
        controller.refresh(ParkingEvidence(park = false), 101)
        controller.complete(first, park(102), 102, 1000, closed())
        assertFalse(controller.state.confirmed)
        val second = controller.begin(park(200), 200)!!
        controller.complete(second, park(15_201), 15_201, 1000, closed())
        assertFalse(controller.state.confirmed)
    }

    @Test fun manualChecksRemainIndependentAndResetClearsAll() {
        var manual = updateManualCheck(emptySet(), 0, true)
        manual = updateManualCheck(manual, DOOR_CHECK_INDEX, true)
        assertEquals(setOf(0, DOOR_CHECK_INDEX), displayedChecklistChecks(manual, true))
        // Losing the SDK result must not erase an independently checked door.
        assertEquals(setOf(0, DOOR_CHECK_INDEX), displayedChecklistChecks(manual, false))
        manual = updateManualCheck(manual, DOOR_CHECK_INDEX, false)
        assertEquals(setOf(0), displayedChecklistChecks(manual, false))
        // The reset button clears manual checks and controller.clear clears SDK state.
        val controller = DoorChecklistController()
        val token = controller.begin(park(0), 0)!!
        controller.complete(token, park(1), 1, 1, closed())
        controller.clear(clearSnapshot = true); manual = emptySet()
        assertTrue(displayedChecklistChecks(manual, controller.state.confirmed).isEmpty())
    }
}
