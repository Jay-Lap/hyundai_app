package com.example.hyundiaboysranger

import android.content.Context

data class LocalSettingsBackup(val settings: UserSettings, val savedAt: Long)

/** One app-private backup slot, separate from current settings and practice history. */
class LocalSettingsBackupRepository(context: Context, name: String = "local_settings_backup") {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun load(): LocalSettingsBackup? {
        val payload = preferences.getString("payload", null) ?: return null
        val settings = SettingsBackup.decode(payload) ?: return null
        val savedAt = preferences.getLong("saved_at", 0)
        return if (savedAt > 0) LocalSettingsBackup(settings, savedAt) else null
    }

    /** Call on IO. Report success only after the disk write succeeds. */
    fun save(settings: UserSettings): Boolean = preferences.edit()
        .putString("payload", SettingsBackup.encode(settings))
        .putLong("saved_at", System.currentTimeMillis()).commit()
}
