package com.example.hyundiaboysranger

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** This test uses only a unique test namespace and leaves it in place. */
class ReviewIntegrityIsolationTest {
    @Test fun incompleteLegacyRecordIsNotMigratedOrOverwritten() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "review_integrity_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        assertTrue(prefs.edit().putBoolean("consent", true).putLong("updated", 123L).commit())
        val before = prefs.all.toMap()
        val repo = ReviewRepository(context, name)
        assertTrue(repo.hasDamagedData())
        assertNull(repo.load())
        assertFalse(repo.migrateLegacy())
        repo.save(true, "test")
        assertEquals(before, prefs.all)
    }
}
