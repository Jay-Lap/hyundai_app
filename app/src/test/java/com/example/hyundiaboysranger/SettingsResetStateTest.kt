package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class SettingsResetStateTest {
    @Test fun resetRequiresOpeningAndClosesOnCancelOrConfirmation() {
        assertTrue(resetConfirmationVisible(false, SettingsResetEvent.REQUEST))
        assertFalse(resetConfirmationVisible(true, SettingsResetEvent.CANCEL))
        assertFalse(resetConfirmationVisible(true, SettingsResetEvent.CONFIRM))
    }
}
