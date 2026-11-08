package com.example.hyundiaboysranger

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Uses a unique test namespace and leaves it in place; user data is never touched. */
class ReviewMigrationIsolationTest {
    @Test fun preservesLegacyAndRefusesFutureVersion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testName = "migration_records_${UUID.randomUUID()}"
        val data = context.getSharedPreferences(testName, 0)
        data.edit().putBoolean("consent", true).putLong("updated", 123L).putBoolean("complete", false)
            .putString("source", "SDK").putString("history", "[{\"complete\":false,\"time\":123,\"source\":\"SDK\"}]").commit()
        val before = data.all.toMap()
        val repo = ReviewRepository(context, testName)
        val record = repo.load()
        val history = repo.history()
        assertEquals(ReviewRecord(false, 123L, "SDK"), record)
        assertEquals(listOf(ReviewRecord(false, 123L, "SDK")), history)
        assertTrue(repo.migrateLegacy())
        assertTrue(repo.migrateLegacy())
        assertEquals(before, data.all.filterKeys { it !in setOf("schema_version", "review_items", "review_history") })
        assertEquals(listOf(ReviewItem("wiper", "front_wiper", "started", 123L)), repo.items())
        assertEquals(listOf(123L), repo.events().map { it.updatedAt })
        assertEquals(record, repo.load())
        assertEquals(history, repo.history())
        data.edit().putInt("schema_version", 99).commit()
        val future = data.all.toMap()
        assertFalse(repo.migrateLegacy())
        repo.save(true, "SDK")
        assertEquals(future, data.all)
    }
}
