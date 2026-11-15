package com.example.hyundiaboysranger

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkingPollEvidenceTest {
    private fun allowed(connected: Boolean = true, started: Long = 100, completed: Long = 101,
                        park: Boolean? = true, speed: Float? = 0f, ux: Boolean? = true): Boolean {
        val evidence = parkingEvidenceFromPoll(connected, started, completed, park, speed, ux)
        return FeatureGate.allowed(AppFeature.PRACTICE, evidence, completed)
    }

    @Test fun parkedValuesPermitOnlyWhenAllReadResultsAreKnown() {
        assertTrue(allowed())
        assertFalse(allowed(park = false))
        assertFalse(allowed(speed = 1f))
        assertFalse(allowed(park = null)) // unavailable or error
        assertFalse(allowed(speed = null)) // unavailable or error
        assertFalse(allowed(ux = null))
    }

    @Test fun longPollFutureStartAndLostConnectionFailClosed() {
        assertFalse(allowed(started = 100, completed = 2601))
        assertFalse(allowed(started = 102, completed = 101))
        assertFalse(allowed(connected = false))
    }

    @Test fun reconnectionRequiresANewSuccessfulPoll() {
        assertFalse(allowed(connected = false))
        assertFalse(allowed(connected = true, park = null))
        assertTrue(allowed(connected = true, started = 5000, completed = 5001))
    }
}
