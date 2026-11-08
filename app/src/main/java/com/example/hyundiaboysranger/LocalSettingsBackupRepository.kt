package com.example.hyundiaboysranger

import android.content.Context
import android.content.SharedPreferences

data class LocalSettingsBackup(val settings: UserSettings, val savedAt: Long)

/** One app-private backup slot, separate from current settings and practice history. */
class LocalSettingsBackupRepository internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context, name: String = "local_settings_backup") : this(
        context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE))
    private val guard = PreferenceWriteGuards.forPreferences(preferences)
    fun isWriteUnknown(): Boolean = guard.uncertain
    fun retryWrite(): Boolean = synchronized(preferences) { guard.retry() == WriteResult.SUCCESS }
    fun hasStoredData(): Boolean = preferences.all.isNotEmpty()

    fun load(): LocalSettingsBackup? {
        if (guard.uncertain) return null
        val values = preferences.all
        val payload = values["payload"] as? String ?: return null
        val settings = SettingsBackup.decode(payload) ?: return null
        val savedAt = values["saved_at"] as? Long ?: 0L
        return if (savedAt > 0) LocalSettingsBackup(settings, savedAt) else null
    }

    /** Call on IO. Report success only after the disk write succeeds. */
    fun save(settings: UserSettings): Boolean = synchronized(preferences) {
        if (guard.uncertain || (hasStoredData() && load() == null)) return@synchronized false
        val payload = SettingsBackup.encode(settings)
        val savedAt = System.currentTimeMillis()
        guard.execute { androidCommit(preferences.edit().putString("payload", payload).putLong("saved_at", savedAt)) } == WriteResult.SUCCESS
    }
}
