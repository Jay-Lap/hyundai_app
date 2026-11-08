package com.example.hyundiaboysranger

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class ReviewWriteFailureTest {
    private val original = mapOf<String, Any>(
        "schema_version" to 1, "consent" to true, "complete" to false, "updated" to 123L,
        "source" to "original", "history" to "[{\"complete\":false,\"time\":123,\"source\":\"original\"}]",
        "other" to "preserve"
    )
    private val backup = mapOf<String, Any>("payload" to SettingsBackup.encode(UserSettings(largeTitle = true)), "saved_at" to 42L)
    private enum class Action { START, COMPLETE, DELETE, REVOKE }
    private fun run(repo: ReviewRepository, action: Action): Boolean = when (action) {
        Action.START -> repo.save(false, "new start")
        Action.COMPLETE -> repo.save(true, "new completion")
        Action.DELETE -> repo.delete()
        Action.REVOKE -> repo.setConsent(false)
    }

    @Test fun everyJsonWriteHandlesFailureExceptionCancelAndExactRetry() {
        for (action in Action.entries) for (throwException in listOf(false, true)) {
            val prefs = FakePreferences(original)
            val separateBackup = FakePreferences(backup)
            val repo = ReviewRepository(prefs)
            val beforeBackup = separateBackup.disk.toMap()
            assertEquals(ReviewStorageStatus.NORMAL, repo.storageStatus())
            assertEquals(1, repo.history().size)
            prefs.reportSuccess = false
            prefs.throwOnCommit = throwException
            assertFalse("$action exception=$throwException", run(repo, action))
            assertEquals(original, prefs.disk)
            assertEquals(ReviewStorageStatus.WRITE_UNKNOWN, repo.storageStatus())
            assertNull(repo.load())
            assertTrue(repo.history().isEmpty())
            val attempted = prefs.all.toMap()
            // Dismissing the retry confirmation does not invoke retry or change disk.
            val writes = prefs.commits
            assertFalse(repo.setConsent(true))
            assertEquals(writes, prefs.commits)
            assertEquals(original, prefs.disk)
            assertEquals(beforeBackup, separateBackup.disk)
            prefs.reportSuccess = true
            prefs.throwOnCommit = false
            assertTrue(repo.retryWrite())
            assertEquals(attempted, prefs.disk)
            assertEquals(beforeBackup, separateBackup.disk)
            if (action == Action.START || action == Action.COMPLETE) {
                val history = JsonParser.parseString(prefs.disk["history"] as String).asJsonArray
                assertEquals(2, history.size())
                assertEquals("original", history[1].asJsonObject["source"].asString)
                assertEquals(action == Action.COMPLETE, history[0].asJsonObject["complete"].asBoolean)
            } else assertFalse(prefs.disk.containsKey("history"))
        }
    }

    @Test fun falseOrExceptionCanStillMeanDiskChangedAndRestartCannotRecoverIntent() {
        for (action in Action.entries) for (reachedDisk in listOf(false, true)) for (throwException in listOf(false, true)) {
            val prefs = FakePreferences(original)
            val repo = ReviewRepository(prefs)
            prefs.reportSuccess = false
            prefs.throwOnCommit = throwException
            prefs.writeDiskOnFailure = reachedDisk
            assertFalse(run(repo, action))
            assertEquals(ReviewStorageStatus.WRITE_UNKNOWN, repo.storageStatus())
            if (!reachedDisk) assertEquals(original, prefs.disk) else assertNotEquals(original, prefs.disk)
            val persisted = prefs.disk.toMap()
            // New store models a fresh process; no operation is replayed or inferred.
            val restarted = FakePreferences(persisted)
            val freshRepo = ReviewRepository(restarted)
            assertEquals(ReviewStorageStatus.NORMAL, freshRepo.storageStatus())
            assertEquals(persisted, restarted.disk)
            assertEquals(0, restarted.commits)
            assertNull(freshRepo.pendingWriteDescription())
        }
    }

    @Test fun successfulJsonHistoryKeepsOldRecordAndHonorsConsent() {
        val prefs = FakePreferences(original)
        val repo = ReviewRepository(prefs)
        assertTrue(repo.save(false, "start"))
        assertTrue(repo.save(true, "complete"))
        assertEquals(listOf(true, false, false), repo.history().map { it.complete })
        assertTrue(repo.delete())
        assertTrue(repo.history().isEmpty())
        assertTrue(repo.setConsent(false))
        assertFalse(repo.save(true, "not consented"))
        assertEquals("preserve", prefs.disk["other"])
    }

    @Test fun restartWithDamagedOrFutureDiskSnapshotRemainsProtected() {
        val damaged = original + ("history" to "not json")
        val future = original + ("schema_version" to 99)
        for ((snapshot, expected) in listOf(damaged to ReviewStorageStatus.DAMAGED, future to ReviewStorageStatus.FUTURE_VERSION)) {
            val prefs = FakePreferences(snapshot)
            val repo = ReviewRepository(prefs)
            assertEquals(expected, repo.storageStatus())
            assertFalse(repo.save(true, "must not overwrite"))
            assertFalse(repo.setConsent(false))
            assertFalse(repo.delete())
            assertEquals(snapshot, prefs.disk)
            assertEquals(0, prefs.commits)
        }
    }
}
