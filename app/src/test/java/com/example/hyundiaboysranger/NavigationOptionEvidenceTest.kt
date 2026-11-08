package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class NavigationOptionEvidenceTest {
    @Test fun mismatchedAndLateCallbacksDoNotCompleteARequest() {
        val initial = NavigationOptionEvidence("RECOMMENDED", "HIGHWAY")
        val wrong = initial.onCallback("FREE")
        assertEquals(OptionPhase.AWAITING_CALLBACK, wrong.phase)
        assertEquals(wrong, wrong.onRequery("HIGHWAY"))
        val interrupted = wrong.interrupted()
        assertEquals(OptionPhase.UNCONFIRMED, interrupted.phase)
        assertEquals(interrupted, interrupted.onCallback("HIGHWAY"))
    }

    @Test fun matchingCallbackNeedsRequeryAndPreservesDifferentObservation() {
        val pending = NavigationOptionEvidence("RECOMMENDED", "HIGHWAY").onCallback("HIGHWAY")
        assertEquals(OptionPhase.AWAITING_REQUERY, pending.phase)
        assertNull(pending.after)
        val observed = pending.onRequery("RECOMMENDED")
        assertEquals(OptionPhase.OBSERVED, observed.phase)
        assertEquals("RECOMMENDED", observed.before)
        assertEquals("HIGHWAY", observed.requested)
        assertEquals("HIGHWAY", observed.callback)
        assertEquals("RECOMMENDED", observed.after)
        assertEquals(observed, observed.onCallback("FREE"))
    }
}
