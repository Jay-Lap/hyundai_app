package com.example.hyundiaboysranger

import android.content.Context

data class ReviewRecord(val complete: Boolean, val updatedAt: Long, val source: String)

/** One local user, one wiper exercise. No vehicle identity or raw telemetry is stored. */
class ReviewRepository(context: Context, name: String = "practice_review") {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    var consent: Boolean
        get() = preferences.getBoolean("consent", false)
        set(value) {
            if (!value) preferences.edit().clear().putBoolean("consent", false).apply()
            else preferences.edit().putBoolean("consent", true).apply()
        }
    fun load(): ReviewRecord? = if (!consent || !preferences.contains("updated")) null else ReviewRecord(
        preferences.getBoolean("complete", false), preferences.getLong("updated", 0),
        preferences.getString("source", "SDK · 환경 미확인") ?: "SDK · 환경 미확인")
    fun save(complete: Boolean, source: String) {
        if (!consent) return
        preferences.edit().putBoolean("complete", complete).putLong("updated", System.currentTimeMillis())
            .putString("source", source).apply()
    }
    fun delete() { preferences.edit().remove("complete").remove("updated").remove("source").apply() }
}
