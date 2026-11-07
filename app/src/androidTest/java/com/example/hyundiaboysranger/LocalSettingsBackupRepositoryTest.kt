package com.example.hyundiaboysranger

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class LocalSettingsBackupRepositoryTest {
    @Test fun backupSurvivesNewRepositoryWithoutMutatingCurrentSettingsOrReview() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "backup_test_${System.nanoTime()}"
        val settingsName = "${name}_settings"
        val reviewName = "${name}_review"
        try {
            val current = SettingsRepository(context, settingsName)
            val review = ReviewRepository(context, reviewName)
            val original = UserSettings(textSizePercent = 149, highContrast = true, detailedGuidance = false)
            current.save(original)
            review.consent = true; review.save(true, "EMU_SDK")
            val record = review.load()
            val history = review.history()
            val backup = LocalSettingsBackupRepository(context, name)
            assertNull(backup.load())
            assertTrue(backup.save(current.load()))
            val changed = original.copy(textSizePercent = 200, displayMode = DisplayMode.NIGHT)
            current.save(changed)
            val loaded = LocalSettingsBackupRepository(context, name).load()!!
            assertEquals(original, loaded.settings)
            assertEquals(changed, current.load()) // Preview is read-only.
            current.save(loaded.settings)
            assertEquals(original, SettingsRepository(context, settingsName).load())
            assertEquals(record, review.load())
            assertEquals(history, review.history())
            assertTrue(backup.save(changed))
            assertEquals(changed, LocalSettingsBackupRepository(context, name).load()!!.settings)
        } finally {
            listOf(name, settingsName, reviewName).forEach { context.deleteSharedPreferences(it) }
        }
    }
}
