package com.example.hyundiaboysranger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PreparationConcernTest {
    @Test fun topicsLeadOnlyToTheirOwnLearningScreensAndDoNotUnlockVehicle() {
        assertEquals(ConcernDestination.DEFOG, PreparationConcern.VISIBILITY.destination)
        assertEquals(ConcernDestination.WIPER, PreparationConcern.WIPER.destination)
        assertEquals(ConcernDestination.WARNING_CATALOG, PreparationConcern.WARNING.destination)
        assertEquals(3, PreparationConcern.entries.map { it.destination }.distinct().size)
        assertFalse(VehicleSdkAccess.enabled)
        assertFalse(ExtraLessonUiState(expanded = true).canConfirmCompletion)
    }
}
