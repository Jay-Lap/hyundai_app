package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class CommonReviewTest {
    private val rawHistory = "[{\"complete\":true,\"time\":40,\"source\":\"old\"},{\"complete\":false,\"time\":20,\"source\":\"older\"}]"
    private fun legacy(versioned: Boolean = true): Map<String, Any> = buildMap {
        if (versioned) put("schema_version", 1)
        put("consent", true); put("complete", true); put("updated", 40L)
        put("source", "old"); put("history", rawHistory); put("other", "keep")
    }

    @Test fun legacyMigrationPreservesOriginalAndIsIdempotent() {
        for (versioned in listOf(false, true)) {
            val original = legacy(versioned)
            val prefs = FakePreferences(original)
            val repo = ReviewRepository(prefs)
            val oldRecord = repo.load(); val oldHistory = repo.history()
            assertTrue(repo.migrateLegacy())
            assertEquals(2, prefs.disk["schema_version"])
            original.filterKeys { it != "schema_version" }.forEach { (k, v) -> assertEquals(v, prefs.disk[k]) }
            assertEquals(rawHistory, prefs.disk["history"])
            assertEquals(oldRecord, repo.load()); assertEquals(oldHistory, repo.history())
            assertEquals(listOf(ReviewItem("wiper", "front_wiper", "complete", 40L)), repo.items())
            assertEquals(listOf("complete", "started"), repo.events().map { it.state })
            assertEquals(listOf(40L, 20L), repo.events().map { it.updatedAt })
            assertEquals(listOf("old", "older"), repo.events().map { it.source })
            val snapshot = prefs.disk.toMap(); val commits = prefs.commits
            assertTrue(repo.migrateLegacy()); assertEquals(commits, prefs.commits); assertEquals(snapshot, prefs.disk)
        }
    }

    @Test fun explicitWarningAddDuplicateDeleteAndWiperAreIndependent() {
        val prefs = FakePreferences(legacy())
        val repo = ReviewRepository(prefs)
        assertTrue(repo.addWarning("engine"))
        assertEquals(rawHistory, prefs.disk["history"])
        assertEquals(setOf("wiper", "warning"), repo.items().map { it.kind }.toSet())
        val once = prefs.disk.toMap(); val commits = prefs.commits
        assertTrue(repo.addWarning("engine")); assertEquals(commits, prefs.commits); assertEquals(once, prefs.disk)
        assertTrue(repo.addWarning("tire"))
        assertTrue(repo.deleteWarning("engine"))
        assertEquals(rawHistory, prefs.disk["history"])
        assertEquals(setOf("front_wiper", "tire"), repo.items().map { it.id }.toSet())
        assertTrue(repo.delete())
        assertEquals(listOf("tire"), repo.items().map { it.id })
        assertFalse(prefs.disk.containsKey("history"))
    }

    @Test fun consentIsRequiredAndRevocationRemovesBothTypesOnlyAfterCall() {
        val prefs = FakePreferences(legacy() + ("consent" to false))
        val repo = ReviewRepository(prefs)
        assertFalse(repo.addWarning("engine")); assertEquals(0, prefs.commits)
        assertTrue(repo.setConsent(true)); assertTrue(repo.addWarning("engine"))
        assertTrue(repo.setConsent(false)); assertTrue(repo.items().isEmpty())
        assertEquals("keep", prefs.disk["other"])
        assertFalse(prefs.disk.containsKey("history"))
        assertTrue(repo.setConsent(true)); assertTrue(repo.items().isEmpty())
    }

    @Test fun failedMigrationAndWarningWriteRequireExactExplicitRetry() {
        for (migration in listOf(true, false)) {
            val prefs = FakePreferences(legacy())
            val repo = ReviewRepository(prefs)
            prefs.reportSuccess = false
            assertFalse(if (migration) repo.migrateLegacy() else repo.addWarning("engine"))
            assertEquals(legacy(), prefs.disk)
            assertEquals(ReviewStorageStatus.WRITE_UNKNOWN, repo.storageStatus())
            assertFalse(repo.addWarning("tire")); assertEquals(1, prefs.commits)
            // Cancel: no retry call, no extra write, and uncertainty remains.
            assertEquals(legacy(), prefs.disk); assertEquals(ReviewStorageStatus.WRITE_UNKNOWN, repo.storageStatus())
            prefs.reportSuccess = true
            assertTrue(repo.retryWrite())
            assertEquals(rawHistory, prefs.disk["history"])
            assertEquals(!migration, repo.items().any { it.id == "engine" })
            assertFalse(repo.items().any { it.id == "tire" })
        }
    }

    @Test fun malformedFutureAndUnknownReservedFieldsAreNeverOverwritten() {
        val invalidItems = "[{\"kind\":\"warning\",\"id\":\"engine\",\"state\":\"saved\",\"updated\":1},{\"kind\":\"warning\",\"id\":\"engine\",\"state\":\"saved\",\"updated\":2}]"
        val cases = listOf(
            legacy() + ("schema_version" to 99),
            legacy() + ("review_items" to "unknown legacy data"),
            legacy() + ("review_history" to "unknown legacy data"),
            legacy() + mapOf("schema_version" to 2, "review_items" to invalidItems),
            legacy() + mapOf("schema_version" to 2, "review_items" to "[]")
        )
        cases.forEach { values ->
            val prefs = FakePreferences(values); val repo = ReviewRepository(prefs)
            assertTrue(repo.storageStatus() != ReviewStorageStatus.NORMAL)
            assertFalse(repo.migrateLegacy()); assertFalse(repo.addWarning("engine")); assertFalse(repo.delete())
            assertEquals(values, prefs.disk); assertEquals(0, prefs.commits)
        }
    }

    @Test fun migrationAndLaterPracticeKeepExtraLegacyHistoryFields() {
        val raw = "[{\"complete\":true,\"time\":40,\"source\":\"old\",\"extra\":\"preserve\"}]"
        val prefs = FakePreferences(legacy() + ("history" to raw))
        val repo = ReviewRepository(prefs)
        assertTrue(repo.migrateLegacy())
        assertEquals(raw, prefs.disk["history"])
        assertTrue(repo.events().single().original.contains("preserve"))
        assertTrue(repo.save(false, "new"))
        assertTrue((prefs.disk["history"] as String).contains("preserve"))
        assertTrue(repo.events()[1].original.contains("preserve"))
    }

    @Test fun mismatchedCommonHistoryIsProtected() {
        val initial = FakePreferences(legacy()); val first = ReviewRepository(initial)
        assertTrue(first.migrateLegacy())
        val corrupted = initial.disk.toMap() + ("review_history" to "[]")
        val prefs = FakePreferences(corrupted); val repo = ReviewRepository(prefs)
        assertEquals(ReviewStorageStatus.DAMAGED, repo.storageStatus())
        assertFalse(repo.addWarning("engine")); assertFalse(repo.save(true, "new"))
        assertEquals(corrupted, prefs.disk); assertEquals(0, prefs.commits)
    }

    @Test fun warningDeletionFailureAndExceptionsDoNotOverwriteUnrelatedData() {
        for (deleting in listOf(false, true)) for (exception in listOf(false, true)) {
            val prefs = FakePreferences(legacy()); val repo = ReviewRepository(prefs)
            assertTrue(repo.addWarning("tire"))
            val before = prefs.disk.toMap()
            prefs.reportSuccess = false; prefs.throwOnCommit = exception
            assertFalse(if (deleting) repo.deleteWarning("tire") else repo.addWarning("engine"))
            assertEquals(before, prefs.disk)
            assertEquals(ReviewStorageStatus.WRITE_UNKNOWN, repo.storageStatus())
            assertTrue(repo.items().isEmpty())
            // Cancelling does not call retry and cannot assert rollback.
            assertEquals(before, prefs.disk)
            prefs.reportSuccess = true; prefs.throwOnCommit = false
            assertTrue(repo.retryWrite())
            assertEquals(rawHistory, prefs.disk["history"])
            assertEquals(!deleting, repo.items().any { it.id == "tire" })
            assertEquals(!deleting, repo.items().any { it.id == "engine" })
        }
    }
}
