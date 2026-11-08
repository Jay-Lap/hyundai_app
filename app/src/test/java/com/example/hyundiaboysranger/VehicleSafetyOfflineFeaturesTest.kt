package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class VehicleSafetyOfflineFeaturesTest {
    @Test fun parkedOfflineActionsAndFakeStoresRemainUsable() {
        assertFalse(VehicleSdkAccess.enabled)
        val parked = ParkingEvidence(true, 0f, true, 100)
        listOf(AppFeature.SETTINGS, AppFeature.CHECKLIST, AppFeature.REVIEW, AppFeature.PRACTICE).forEach {
            assertTrue(FeatureGate.allowed(it, parked, 100))
            assertFalse(FeatureGate.allowed(it, parked.copy(park = null), 100))
        }
        val settings = SettingsRepository(FakePreferences())
        assertTrue(settings.save(UserSettings(highContrast = true)))
        assertTrue(settings.load().highContrast)
        val review = ReviewRepository(FakePreferences(mapOf("consent" to true)))
        val catalog = parseWarningCatalog("engine\t엔진\t설명\t확인")
        assertEquals("engine", catalog.single().id)
        assertTrue(review.addWarning("engine"))
        assertEquals(listOf("engine"), review.items().map { it.id })
        assertTrue(DOOR_CHECK_INDEX in updateManualCheck(emptySet(), DOOR_CHECK_INDEX, true))
    }

    @Test fun climateAndDefogRemainInformationalOnly() {
        ExtraLesson.entries.forEach {
            val expanded = reduceExtraLesson(ExtraLessonUiState(), ExtraLessonUiEvent.Toggle)
            assertTrue(expanded.expanded)
            assertFalse(expanded.canConfirmCompletion)
        }
    }
}
