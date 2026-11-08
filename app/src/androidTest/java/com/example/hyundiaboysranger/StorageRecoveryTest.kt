package com.example.hyundiaboysranger

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class StorageRecoveryTest {
    @Test fun legacyReviewMigrationPreservesRecordsAndRejectsFutureSchema() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration_review_${UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(name, 0)
        try {
            val history = "[{\"complete\":false,\"time\":123,\"source\":\"SDK\"}]"
            preferences.edit().putBoolean("consent", true).putBoolean("complete", false)
                .putLong("updated", 123L).putString("source", "SDK").putString("history", history).commit()
            val before = preferences.all.toMap()
            val repo = ReviewRepository(context, name)
            val record = repo.load()
            val items = repo.history()
            assertTrue(repo.migrateLegacy())
            assertTrue(repo.migrateLegacy())
            assertEquals(before, preferences.all.filterKeys { it !in setOf("schema_version", "review_items", "review_history") })
            assertEquals(listOf(ReviewItem("wiper", "front_wiper", "started", 123L)), repo.items())
            assertEquals(listOf(123L), repo.events().map { it.updatedAt })
            assertEquals(record, repo.load())
            assertEquals(items, repo.history())
            preferences.edit().putInt("schema_version", 99).commit()
            val future = preferences.all.toMap()
            assertFalse(repo.migrateLegacy())
            repo.save(true, "SDK")
            assertEquals(future, preferences.all)
        } finally { context.deleteSharedPreferences(name) }
    }
    @Test fun damagedSettingFieldFallsBackWithoutRewritingOtherFields() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "recovery_settings_${UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(name, 0)
        try {
            preferences.edit().putString("profile", "senior").putString("text_size_percent", "broken")
                .putBoolean("prefer_highways", true).commit()
            val before = preferences.all.toMap()
            val settings = SettingsRepository(context, name).load()
            assertTrue(settings.highContrast)
            assertFalse(settings.detailedGuidance)
            assertTrue(settings.preferHighways)
            assertNull(settings.textSizePercent)
            assertEquals(before, preferences.all)
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun damagedReviewIsPreservedAndCannotBeOverwrittenByPractice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "recovery_review_${UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(name, 0)
        try {
            preferences.edit().putBoolean("consent", true).putString("history", "not json")
                .putLong("updated", 123L).putBoolean("complete", false).putString("source", "test").commit()
            val before = preferences.all.toMap()
            val repository = ReviewRepository(context, name)
            assertTrue(repository.hasDamagedData())
            assertNull(repository.load())
            repository.save(true, "test")
            assertEquals(before, preferences.all)
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun damagedBackupFieldsDoNotThrowOrChangeSavedData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "recovery_backup_${UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(name, 0)
        try {
            preferences.edit().putInt("payload", 5).putString("saved_at", "invalid").commit()
            val before = preferences.all.toMap()
            assertNull(LocalSettingsBackupRepository(context, name).load())
            assertEquals(before, preferences.all)
        } finally { context.deleteSharedPreferences(name) }
    }
}
