package com.example.hyundiaboysranger

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Always uses a unique store and never removes an existing preference file. */
class CommonReviewIsolationTest {
    @Test fun legacyWiperSurvivesMigrationAndWarningChanges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "common_review_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        val history = "[{\"complete\":false,\"time\":20,\"source\":\"legacy\"}]"
        assertTrue(prefs.edit().putInt("schema_version", 1).putBoolean("consent", true)
            .putBoolean("complete", false).putLong("updated", 20L).putString("source", "legacy")
            .putString("history", history).putString("other", "keep").commit())
        val before = prefs.all.toMap()
        val repository = ReviewRepository(context, name)
        assertTrue(repository.migrateLegacy()); val migrated = prefs.all.toMap()
        assertTrue(repository.migrateLegacy()); assertEquals(migrated, prefs.all)
        assertEquals(before.filterKeys { it != "schema_version" }, prefs.all.filterKeys { it !in setOf("schema_version", "review_items", "review_history") })
        assertEquals(listOf(20L), repository.events().map { it.updatedAt })
        assertTrue(repository.addWarning("engine")); assertTrue(repository.addWarning("engine"))
        assertEquals(1, repository.items().count { it.id == "engine" })
        assertTrue(repository.deleteWarning("engine"))
        assertEquals(history, prefs.getString("history", null))
        assertEquals(ReviewRecord(false, 20L, "legacy"), repository.load())
        assertEquals("keep", prefs.getString("other", null))
    }
}
