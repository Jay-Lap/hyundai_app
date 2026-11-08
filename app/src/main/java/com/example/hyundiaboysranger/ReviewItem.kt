package com.example.hyundiaboysranger

import org.json.JSONArray
import org.json.JSONObject

internal data class ReviewItem(val kind: String, val id: String, val state: String, val updatedAt: Long)
internal data class ReviewEvent(val kind: String, val id: String, val state: String, val updatedAt: Long, val source: String, val original: String)

/** Null means malformed or unsupported data; callers must not overwrite it. */
internal fun decodeReviewItems(raw: Any?): List<ReviewItem>? {
    if (raw !is String || raw.length > 65_536) return null
    return try {
        val array = JSONArray(raw)
        if (array.length() > 256) return null
        val result = mutableListOf<ReviewItem>()
        val keys = mutableSetOf<Pair<String, String>>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: return null
            val fields = item.keys().asSequence().toSet()
            if (fields != setOf("kind", "id", "state", "updated")) return null
            val kind = item.opt("kind") as? String ?: return null
            val id = item.opt("id") as? String ?: return null
            val state = item.opt("state") as? String ?: return null
            if (item.opt("updated") !is Number) return null
            val updated = item.optLong("updated", 0)
            if (updated <= 0 || !id.matches(Regex("[a-z_]{1,80}")) || !keys.add(kind to id)) return null
            if (kind == "wiper" && (id != "front_wiper" || state !in setOf("started", "complete"))) return null
            if (kind == "warning" && state != "saved") return null
            if (kind !in setOf("wiper", "warning")) return null
            result.add(ReviewItem(kind, id, state, updated))
        }
        result
    } catch (_: Exception) { null }
}

internal fun encodeReviewItems(items: List<ReviewItem>): String {
    val result = JSONArray()
    items.forEach { result.put(JSONObject().put("kind", it.kind).put("id", it.id).put("state", it.state).put("updated", it.updatedAt)) }
    return result.toString()
}

/** The original history remains untouched; each event also carries its complete original JSON. */
internal fun legacyReviewEvents(raw: Any?): List<ReviewEvent>? {
    if (raw == null) return emptyList()
    if (raw !is String || raw.length > 65_536) return null
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { index ->
            val item = array.optJSONObject(index) ?: return null
            val complete = item.opt("complete") as? Boolean ?: return null
            if (item.opt("time") !is Number) return null
            val time = item.optLong("time", 0)
            val source = item.opt("source") as? String ?: return null
            if (time <= 0) return null
            ReviewEvent("wiper", "front_wiper", if (complete) "complete" else "started", time, source, item.toString())
        }
    } catch (_: Exception) { null }
}

internal fun encodeReviewEvents(events: List<ReviewEvent>): String {
    val array = JSONArray()
    events.forEach { array.put(JSONObject().put("kind", it.kind).put("id", it.id).put("state", it.state)
        .put("updated", it.updatedAt).put("source", it.source).put("original", it.original)) }
    return array.toString()
}

internal fun decodeReviewEvents(raw: Any?): List<ReviewEvent>? {
    if (raw !is String || raw.length > 524_288) return null
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { index ->
            val item = array.optJSONObject(index) ?: return null
            if (item.keys().asSequence().toSet() != setOf("kind", "id", "state", "updated", "source", "original")) return null
            val kind = item.opt("kind") as? String ?: return null
            val id = item.opt("id") as? String ?: return null
            val state = item.opt("state") as? String ?: return null
            val source = item.opt("source") as? String ?: return null
            val original = item.opt("original") as? String ?: return null
            if (item.opt("updated") !is Number) return null
            val updated = item.optLong("updated", 0)
            if (kind != "wiper" || id != "front_wiper" || state !in setOf("started", "complete") || updated <= 0) return null
            ReviewEvent(kind, id, state, updated, source, original)
        }
    } catch (_: Exception) { null }
}
