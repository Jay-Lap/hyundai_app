package com.example.hyundiaboysranger

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class ReviewRepositoryTest {
    @Test fun consentRestoreAndDeletion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "review_test_${System.nanoTime()}"
        val repository = ReviewRepository(context, name)
        try {
            repository.save(false, "EMU_SDK")
            assertNull(repository.load())
            repository.consent = true
            repository.save(false, "EMU_SDK")
            assertFalse(ReviewRepository(context, name).load()!!.complete)
            repository.save(true, "EMU_SDK")
            assertTrue(ReviewRepository(context, name).load()!!.complete)
            repository.delete()
            assertNull(ReviewRepository(context, name).load())
            repository.save(false, "EMU_SDK")
            repository.consent = false
            repository.consent = true
            assertNull(repository.load())
        } finally { context.deleteSharedPreferences(name) }
    }
    @Test fun routePreferencesAreIndependentAndPersisted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "route_test_${System.nanoTime()}"
        try {
            val repository = SettingsRepository(context, name)
            val settings = UserSettings(preferWideRoads = true, preferHighways = false)
            repository.save(settings)
            assertEquals(settings, SettingsRepository(context, name).load())
            repository.save(settings.copy(preferHighways = true))
            assertTrue(repository.load().preferWideRoads)
            assertTrue(repository.load().preferHighways)
        } finally { context.deleteSharedPreferences(name) }
    }
    @Test fun displaySettingsRestoreAndOldSeniorSettingsMigrate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "display_test_${System.nanoTime()}"
        try {
            context.getSharedPreferences(name, 0).edit().putString("profile", "senior")
                .putBoolean("large_title", true).commit()
            val migrated = SettingsRepository(context, name).load()
            assertTrue(migrated.highContrast)
            assertFalse(migrated.detailedGuidance)
            val custom = migrated.copy(profile = UserProfile.CUSTOM, highContrast = false,
                detailedGuidance = true, displayMode = DisplayMode.NIGHT)
            SettingsRepository(context, name).save(custom)
            assertEquals(custom, SettingsRepository(context, name).load())
        } finally { context.deleteSharedPreferences(name) }
    }
}
