package com.example.hyundiaboysranger

import android.content.Context
import android.annotation.SuppressLint
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class ReviewRecord(val complete: Boolean, val updatedAt: Long, val source: String)
enum class ReviewStorageStatus { NORMAL, DAMAGED, FUTURE_VERSION, WRITE_UNKNOWN }
data class ReviewRecoverySnapshot internal constructor(val values: Map<String, *>)

/** One local user; the old wiper fields and history remain the authoritative legacy record. */
class ReviewRepository internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context, name: String = "practice_review") : this(
        context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE))
    private val guard = PreferenceWriteGuards.forPreferences(preferences)
    private val currentSchema = 2
    fun retryWrite(): Boolean = synchronized(preferences) { guard.retry() == WriteResult.SUCCESS }
    fun pendingWriteDescription(): String? = guard.pendingLabel
    /** Legacy fields remain in place. Future or malformed versions are never downgraded. */
    @SuppressLint("UseKtx") // commit's Boolean result is required by this API.
    fun migrateLegacy(): Boolean = synchronized(preferences) {
        if (guard.uncertain) return@synchronized false
        val values = preferences.all
        val version = values["schema_version"]
        if (storageStatus() != ReviewStorageStatus.NORMAL) return@synchronized false
        if (version == currentSchema) return@synchronized true
        val items = legacyItems(values)
        val encoded = encodeReviewItems(items)
        val events = checkedEventEncoding(values["history"]) ?: return@synchronized false
        guard.execute("복습 기록 형식 갱신") { androidCommit(preferences.edit().putString("review_items", encoded).putString("review_history", events).putInt("schema_version", currentSchema)) } == WriteResult.SUCCESS
    }
    /** Unknown newer schemas are distinct from damaged data and remain read-only. */
    fun storageStatus(): ReviewStorageStatus = synchronized(preferences) {
        if (guard.uncertain) return@synchronized ReviewStorageStatus.WRITE_UNKNOWN
        val values = preferences.all
        val version = values["schema_version"]
        if (version is Int && version > currentSchema) return@synchronized ReviewStorageStatus.FUTURE_VERSION
        if (invalidReviewHeader(values, currentSchema)) return@synchronized ReviewStorageStatus.DAMAGED
        if (version != currentSchema && (values.containsKey("review_items") || values.containsKey("review_history"))) return@synchronized ReviewStorageStatus.DAMAGED
        if (version == currentSchema) {
            val items = decodeReviewItems(values["review_items"]) ?: return@synchronized ReviewStorageStatus.DAMAGED
            if (items.filter { it.kind == "wiper" } != legacyItems(values)) return@synchronized ReviewStorageStatus.DAMAGED
        }
        val events = legacyReviewEvents(values["history"]) ?: return@synchronized ReviewStorageStatus.DAMAGED
        if (version == currentSchema && decodeReviewEvents(values["review_history"]) != events) return@synchronized ReviewStorageStatus.DAMAGED
        ReviewStorageStatus.NORMAL
    }
    fun hasDamagedData(): Boolean = storageStatus() == ReviewStorageStatus.DAMAGED

    private fun legacyItems(values: Map<String, *>): List<ReviewItem> {
        val updated = values["updated"] as? Long ?: return emptyList()
        val complete = values["complete"] as? Boolean ?: return emptyList()
        return listOf(ReviewItem("wiper", "front_wiper", if (complete) "complete" else "started", updated))
    }
    private fun itemsFromValidStorage(): List<ReviewItem> =
        if (preferences.all["schema_version"] == currentSchema) decodeReviewItems(preferences.all["review_items"])!!
        else legacyItems(preferences.all)
    private fun checkedEventEncoding(raw: Any?): String? {
        val events = legacyReviewEvents(raw) ?: return null
        val encoded = encodeReviewEvents(events)
        return if (decodeReviewEvents(encoded) == events) encoded else null
    }
    internal fun items(): List<ReviewItem> = synchronized(preferences) {
        if (!consent || storageStatus() != ReviewStorageStatus.NORMAL) emptyList() else itemsFromValidStorage()
    }
    internal fun events(): List<ReviewEvent> = synchronized(preferences) {
        if (!consent || storageStatus() != ReviewStorageStatus.NORMAL) emptyList()
        else if (preferences.all["schema_version"] == currentSchema) decodeReviewEvents(preferences.all["review_history"])!!
        else legacyReviewEvents(preferences.all["history"])!!
    }
    internal fun overview(): ReviewOverview? = synchronized(preferences) {
        if (!consent || storageStatus() != ReviewStorageStatus.NORMAL) null
        else ReviewOverview(itemsFromValidStorage(), if (preferences.all["schema_version"] == currentSchema)
            decodeReviewEvents(preferences.all["review_history"])!! else legacyReviewEvents(preferences.all["history"])!!)
    }

    /** Read-only copy; used for user review and to reject a stale confirmation. */
    fun recoverySnapshot(): ReviewRecoverySnapshot? = synchronized(preferences) {
        if (storageStatus() != ReviewStorageStatus.DAMAGED) null
        else ReviewRecoverySnapshot(preferences.all.mapValues { (_, value) ->
            if (value is Set<*>) value.toSet() else value
        })
    }

    /** Caller must first show this exact snapshot and obtain explicit confirmation. */
    @SuppressLint("UseKtx")
    fun resetDamagedAfterConfirmation(snapshot: ReviewRecoverySnapshot): Boolean = synchronized(preferences) {
        if (storageStatus() != ReviewStorageStatus.DAMAGED || preferences.all != snapshot.values) return@synchronized false
        val removeConsent = preferences.all["consent"] !is Boolean
        guard.execute("손상된 복습 기록 초기화") {
            val editor = preferences.edit()
            listOf("complete", "updated", "source", "history", "schema_version", "review_items", "review_history").forEach(editor::remove)
            if (removeConsent) editor.remove("consent")
            androidCommit(editor)
        } == WriteResult.SUCCESS
    }
    var consent: Boolean
        get() = preferences.all["consent"] as? Boolean ?: false
        set(value) { setConsent(value) }
    fun setConsent(value: Boolean): Boolean = synchronized(preferences) {
        if (storageStatus() != ReviewStorageStatus.NORMAL) return@synchronized false
        val hasCommonSchema = preferences.all["schema_version"] == currentSchema
        guard.execute(if (value) "복습 저장 동의" else "복습 저장 끄기 및 기록 삭제") {
            val editor = preferences.edit().putBoolean("consent", value)
            if (!value) {
                listOf("complete", "updated", "source", "history").forEach(editor::remove)
                if (hasCommonSchema) editor.putString("review_items", "[]").putString("review_history", "[]")
            }
            androidCommit(editor)
        } == WriteResult.SUCCESS
    }
    fun load(): ReviewRecord? = synchronized(preferences) {
        if (!consent || storageStatus() != ReviewStorageStatus.NORMAL || !preferences.contains("updated")) null else ReviewRecord(
            preferences.getBoolean("complete", false), preferences.getLong("updated", 0),
            preferences.getString("source", "SDK · 환경 미확인") ?: "SDK · 환경 미확인")
    }
    fun history(): List<ReviewRecord> = synchronized(preferences) {
        if (!consent || storageStatus() != ReviewStorageStatus.NORMAL) return@synchronized emptyList()
        try {
            val array = JSONArray(preferences.getString("history", "[]"))
            (0 until minOf(array.length(), 20)).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val timestamp = item.optLong("time", 0)
                if (timestamp <= 0) null else ReviewRecord(item.getBoolean("complete"), timestamp, item.getString("source"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save(complete: Boolean, source: String): Boolean = saveRecord(complete, source, null)

    internal fun saveAttempt(id: String, phase: String, source: String): Boolean = synchronized(preferences) {
        if (runCatching { java.util.UUID.fromString(id).toString() != id }.getOrDefault(true) || phase !in setOf("start", "complete", "stop")) return@synchronized false
        if (!consent || storageStatus() != ReviewStorageStatus.NORMAL) return@synchronized false
        if (phase != "start") {
            val latest = events().firstOrNull() ?: return@synchronized false
            if (attemptMarker(latest) != AttemptMarker(id, "start")) return@synchronized false
        } else if (events().any { attemptMarker(it)?.id == id }) return@synchronized false
        saveRecord(phase == "complete", source, AttemptMarker(id, phase))
    }

    private fun saveRecord(complete: Boolean, source: String, marker: AttemptMarker?): Boolean = synchronized(preferences) {
        if (!consent || storageStatus() != ReviewStorageStatus.NORMAL) return@synchronized false
        val timestamp = System.currentTimeMillis()
        val array = JSONArray()
        val entry = JSONObject().put("complete", complete).put("time", timestamp).put("source", source)
        if (marker != null) entry.put("attempt_format", "explicit_stop_v1").put("attempt_id", marker.id).put("attempt_phase", marker.phase)
        array.put(entry)
        val oldHistory = JSONArray(preferences.getString("history", "[]"))
        for (index in 0 until minOf(oldHistory.length(), 19)) array.put(oldHistory.optJSONObject(index))
        val serialized = array.toString()
        val otherItems = itemsFromValidStorage().filterNot { it.kind == "wiper" }
        if (otherItems.size >= 256) return@synchronized false
        val items = otherItems +
            ReviewItem("wiper", "front_wiper", if (complete) "complete" else "started", timestamp)
        val encodedItems = encodeReviewItems(items)
        val encodedEvents = checkedEventEncoding(serialized) ?: return@synchronized false
        guard.execute("복습 기록 저장") { androidCommit(preferences.edit().putString("history", serialized).putBoolean("complete", complete).putLong("updated", timestamp)
            .putString("source", source).putString("review_items", encodedItems).putString("review_history", encodedEvents).putInt("schema_version", currentSchema)) } == WriteResult.SUCCESS
    }
    fun delete(): Boolean = synchronized(preferences) {
        if (storageStatus() != ReviewStorageStatus.NORMAL) return@synchronized false
        val items = encodeReviewItems(itemsFromValidStorage().filterNot { it.kind == "wiper" })
        guard.execute("와이퍼 복습 기록 삭제") { androidCommit(preferences.edit().remove("complete").remove("updated").remove("source").remove("history").putString("review_items", items).putString("review_history", "[]").putInt("schema_version", currentSchema)) } == WriteResult.SUCCESS
    }
    internal fun addWarning(id: String): Boolean = synchronized(preferences) {
        if (!consent || storageStatus() != ReviewStorageStatus.NORMAL || !id.matches(Regex("[a-z]{1,80}"))) return@synchronized false
        val existing = itemsFromValidStorage()
        if (existing.any { it.kind == "warning" && it.id == id }) return@synchronized true
        if (existing.count { it.kind == "warning" } >= 255) return@synchronized false
        val encoded = encodeReviewItems(existing + ReviewItem("warning", id, "saved", System.currentTimeMillis()))
        val encodedEvents = checkedEventEncoding(preferences.all["history"]) ?: return@synchronized false
        guard.execute("경고등 복습 추가") { androidCommit(preferences.edit().putString("review_items", encoded).putString("review_history", encodedEvents).putInt("schema_version", currentSchema)) } == WriteResult.SUCCESS
    }
    internal fun deleteWarning(id: String): Boolean = synchronized(preferences) {
        if (!consent || storageStatus() != ReviewStorageStatus.NORMAL) return@synchronized false
        val existing = itemsFromValidStorage()
        if (existing.none { it.kind == "warning" && it.id == id }) return@synchronized false
        val encoded = encodeReviewItems(existing.filterNot { it.kind == "warning" && it.id == id })
        val encodedEvents = checkedEventEncoding(preferences.all["history"]) ?: return@synchronized false
        guard.execute("경고등 복습 삭제") { androidCommit(preferences.edit().putString("review_items", encoded).putString("review_history", encodedEvents).putInt("schema_version", currentSchema)) } == WriteResult.SUCCESS
    }
}
