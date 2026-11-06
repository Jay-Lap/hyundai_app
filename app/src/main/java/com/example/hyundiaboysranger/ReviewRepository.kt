package com.example.hyundiaboysranger

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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
    fun history(): List<ReviewRecord> {
        if (!consent) return emptyList()
        return try {
            val array = JSONArray(preferences.getString("history", "[]"))
            (0 until minOf(array.length(), 20)).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val timestamp = item.optLong("time", 0)
                if (timestamp <= 0) null else ReviewRecord(item.getBoolean("complete"), timestamp, item.getString("source"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save(complete: Boolean, source: String) {
        if (!consent) return
        val timestamp = System.currentTimeMillis()
        val records = (listOf(ReviewRecord(complete, timestamp, source)) + history()).take(20)
        val array = JSONArray()
        records.forEach { record -> array.put(JSONObject().put("complete", record.complete)
            .put("time", record.updatedAt).put("source", record.source)) }
        preferences.edit().putString("history", array.toString()).putBoolean("complete", complete).putLong("updated", timestamp)
            .putString("source", source).apply()
    }
    fun delete() { preferences.edit().remove("complete").remove("updated").remove("source").remove("history").apply() }
}
