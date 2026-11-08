package com.example.hyundiaboysranger

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

/** Independent memory/disk model: false can mean no disk write or a completed disk write. */
internal class FakePreferences(initial: Map<String, Any> = emptyMap()) : SharedPreferences {
    val disk = initial.toMutableMap()
    private val memory = initial.toMutableMap()
    var reportSuccess = true
    var writeDiskOnFailure = false
    var throwOnCommit = false
    var commits = 0
    override fun getAll(): MutableMap<String, *> = memory.toMutableMap()
    override fun contains(key: String?) = memory.containsKey(key)
    override fun getString(key: String?, defValue: String?) = memory[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?) = memory[key] as? MutableSet<String> ?: defValues
    override fun getInt(key: String?, defValue: Int) = memory[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long) = memory[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float) = memory[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean) = memory[key] as? Boolean ?: defValue
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = Editor()

    private inner class Editor : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private var clear = false
        override fun putString(key: String, value: String?) = apply { changes[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = apply { changes[key] = values?.toSet() }
        override fun putInt(key: String, value: Int) = apply { changes[key] = value }
        override fun putLong(key: String, value: Long) = apply { changes[key] = value }
        override fun putFloat(key: String, value: Float) = apply { changes[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }
        override fun remove(key: String) = apply { changes[key] = null }
        override fun clear() = apply { clear = true }
        override fun apply() { error("asynchronous writes are not allowed") }
        override fun commit(): Boolean {
            commits++
            if (clear) memory.clear()
            changes.forEach { (key, value) -> if (value == null) memory.remove(key) else memory[key] = value }
            if (reportSuccess || writeDiskOnFailure) { disk.clear(); disk.putAll(memory) }
            if (throwOnCommit) throw IllegalStateException("injected write failure")
            return reportSuccess
        }
    }
}

class PreferenceFailureTest {
    @Test fun settingsFailureIsSharedAndRetryWritesTheOriginalValue() {
        val prefs = FakePreferences()
        val first = SettingsRepository(prefs)
        val second = SettingsRepository(prefs)
        prefs.reportSuccess = false
        assertFalse(first.save(UserSettings(largeTitle = true)))
        assertEquals(SettingsStorageKind.WRITE_UNKNOWN, second.status().kind)
        assertNull(second.loadIfReadable())
        assertFalse(second.save(UserSettings(preferHighways = true)))
        assertTrue(prefs.disk.isEmpty())
        prefs.reportSuccess = true
        assertTrue(second.retryWrite())
        assertEquals(true, prefs.disk["large_title"])
        assertEquals(false, prefs.disk["prefer_highways"])
    }

    @Test fun failedBackupDoesNotTreatMemoryAsDiskAndBlocksReplacement() {
        val old = UserSettings(largeTitle = true)
        val prefs = FakePreferences(mapOf("payload" to SettingsBackup.encode(old), "saved_at" to 5L))
        val repo = LocalSettingsBackupRepository(prefs)
        prefs.reportSuccess = false
        assertFalse(repo.save(UserSettings(preferHighways = true)))
        assertTrue(repo.isWriteUnknown())
        assertNull(repo.load())
        assertEquals(SettingsBackup.encode(old), prefs.disk["payload"])
        assertFalse(repo.save(UserSettings()))
        assertEquals(1, prefs.commits)
        prefs.reportSuccess = true
        assertTrue(repo.retryWrite())
        assertTrue(repo.load()!!.settings.preferHighways)
    }

    @Test fun reviewDeleteUnknownPreservesDiskUntilExplicitRetry() {
        val prefs = FakePreferences(mapOf("consent" to true, "complete" to true, "updated" to 10L, "source" to "test"))
        val repo = ReviewRepository(prefs)
        prefs.reportSuccess = false
        assertFalse(repo.delete())
        assertEquals(ReviewStorageStatus.WRITE_UNKNOWN, repo.storageStatus())
        assertEquals(true, prefs.disk["complete"])
        assertFalse(repo.setConsent(false))
        prefs.reportSuccess = true
        assertTrue(repo.retryWrite())
        assertFalse(prefs.disk.containsKey("complete"))
        assertEquals(true, prefs.disk["consent"])
    }

    @Test fun protectedFutureAndDamagedBackupAreNotOverwritten() {
        val future = FakePreferences(mapOf("schema_version" to 99))
        assertFalse(SettingsRepository(future).save(UserSettings()))
        assertFalse(ReviewRepository(future).setConsent(false))
        assertEquals(0, future.commits)
        val backup = FakePreferences(mapOf("payload" to "damaged", "saved_at" to 1L))
        assertFalse(LocalSettingsBackupRepository(backup).save(UserSettings()))
        assertEquals("damaged", backup.disk["payload"])
        assertEquals(0, backup.commits)
    }

    @Test fun damagedResetRetainsOriginalOnDiskUntilExplicitRetry() {
        val prefs = FakePreferences(mapOf("text_size_percent" to "broken", "extra" to "keep"))
        val repo = SettingsRepository(prefs)
        val snapshot = repo.recoverySnapshot()!!
        prefs.reportSuccess = false
        assertFalse(repo.resetDamagedAfterConfirmation(snapshot))
        assertEquals("broken", prefs.disk["text_size_percent"])
        assertEquals(SettingsStorageKind.WRITE_UNKNOWN, repo.status().kind)
        assertNull(repo.recoverySnapshot())
        prefs.reportSuccess = true
        assertTrue(repo.retryWrite())
        assertEquals("keep", prefs.disk["extra"])
        assertEquals(SettingsStorageKind.CURRENT, repo.status().kind)
    }

    @Test fun reviewRecoveryFailureAndConsentDisableKeepUnknownFields() {
        val prefs = FakePreferences(mapOf("complete" to true, "consent" to true, "other" to "keep"))
        val repo = ReviewRepository(prefs)
        val snapshot = repo.recoverySnapshot()!!
        prefs.reportSuccess = false
        assertFalse(repo.resetDamagedAfterConfirmation(snapshot))
        assertEquals(true, prefs.disk["complete"])
        assertEquals(ReviewStorageStatus.WRITE_UNKNOWN, repo.storageStatus())
        prefs.reportSuccess = true
        assertTrue(repo.retryWrite())
        assertTrue(repo.setConsent(false))
        assertEquals("keep", prefs.disk["other"])
        assertEquals(false, prefs.disk["consent"])
    }

    @Test fun failureMayHaveChangedDiskAndRemainsUnknown() {
        val prefs = FakePreferences()
        val repo = SettingsRepository(prefs)
        prefs.reportSuccess = false
        prefs.writeDiskOnFailure = true
        assertFalse(repo.save(UserSettings(largeTitle = true)))
        assertEquals(true, prefs.disk["large_title"])
        assertEquals(SettingsStorageKind.WRITE_UNKNOWN, repo.status().kind)
        assertFalse(repo.save(UserSettings()))
    }

    @Test fun simulatedProcessRestartReadsEitherDiskOutcomeWithoutRetrying() {
        for (writeReachedDisk in listOf(false, true)) {
            val original = FakePreferences()
            val repo = SettingsRepository(original)
            original.reportSuccess = false
            original.writeDiskOnFailure = writeReachedDisk
            assertFalse(repo.save(UserSettings(largeTitle = true)))
            // A fresh FakePreferences models a new process loading the disk snapshot.
            val restarted = FakePreferences(original.disk.toMap())
            val restartedRepo = SettingsRepository(restarted)
            assertEquals(writeReachedDisk, restartedRepo.load().largeTitle)
            assertEquals(0, restarted.commits)
        }
    }
}
