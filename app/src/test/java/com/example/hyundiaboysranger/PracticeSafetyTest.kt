package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class PracticeSafetyTest {
    @Test fun requiresFullOffOnOffSequence() {
        var state = PracticeProgress.WAIT_OFF
        state = state.observe(true)
        assertEquals(PracticeProgress.WAIT_OFF, state)
        state = state.observe(false).observe(true)
        assertEquals(PracticeProgress.WAIT_FINAL_OFF, state)
        assertEquals(PracticeProgress.COMPLETE, state.observe(false))
    }
    @Test fun lostSignalInvalidatesPartialProgress() {
        val state = PracticeProgress.WAIT_FINAL_OFF.observe(null).observe(false)
        assertEquals(PracticeProgress.WAIT_ON, state)
        assertNotEquals(PracticeProgress.COMPLETE, state.observe(false))
    }
    @Test fun parkingRequiresAllSignalsAndFreshObservation() {
        val parked = ParkingEvidence(true, 0f, true, 100)
        assertTrue(parked.allowsPractice(101))
        assertFalse(parked.allowsPractice(2601))
        assertFalse(parked.allowsPractice(99))
        assertFalse(parked.copy(park = false).allowsPractice(101))
        assertFalse(parked.copy(park = null).allowsPractice(101))
        assertFalse(parked.copy(speed = null).allowsPractice(101))
        assertFalse(parked.copy(speed = 1f).allowsPractice(101))
        assertFalse(parked.copy(speed = Float.NaN).allowsPractice(101))
        assertFalse(parked.copy(speed = Float.POSITIVE_INFINITY).allowsPractice(101))
        assertFalse(parked.copy(unrestricted = false).allowsPractice(101))
        assertFalse(parked.copy(unrestricted = null).allowsPractice(101))
        assertFalse(ParkingEvidence().allowsPractice(101))
    }
}
