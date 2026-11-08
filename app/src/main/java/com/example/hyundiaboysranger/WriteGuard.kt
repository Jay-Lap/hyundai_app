package com.example.hyundiaboysranger

/** FAILED means rejected before any write. UNKNOWN means a write may have reached disk. */
internal enum class WriteResult { SUCCESS, FAILED, UNKNOWN }

/** A store must report UNKNOWN when it cannot prove whether a write reached disk. */
internal fun interface PersistentWrite {
    fun write(): WriteResult
}

/** Holds the exact attempted operation until it succeeds. Cancellation never rolls data back. */
internal class WriteGuard {
    private var pending: PersistentWrite? = null
    private var label: String? = null
    private val listeners = mutableSetOf<() -> Unit>()
    val uncertain: Boolean @Synchronized get() = pending != null
    val pendingLabel: String? @Synchronized get() = label

    @Synchronized fun addListener(listener: () -> Unit) { listeners.add(listener) }
    @Synchronized fun removeListener(listener: () -> Unit) { listeners.remove(listener) }

    @Synchronized fun execute(operation: PersistentWrite): WriteResult = execute("저장", operation)
    @Synchronized fun execute(label: String, operation: PersistentWrite): WriteResult {
        if (pending != null) return WriteResult.UNKNOWN
        this.label = label
        return attempt(operation, false)
    }

    @Synchronized fun retry(): WriteResult = pending?.let { attempt(it, true) } ?: WriteResult.FAILED

    private fun attempt(operation: PersistentWrite, wasUncertain: Boolean): WriteResult {
        // Install the guard before calling the store: listeners may run during commit.
        pending = operation
        val reported = try { operation.write() } catch (_: Exception) { WriteResult.UNKNOWN }
        // A rejected retry cannot resolve the earlier, possibly completed write.
        val result = if (wasUncertain && reported == WriteResult.FAILED) WriteResult.UNKNOWN else reported
        if (result != WriteResult.UNKNOWN) { pending = null; label = null }
        listeners.toList().forEach { it() }
        return result
    }
}

internal object PreferenceWriteGuards {
    private val guards = java.util.WeakHashMap<android.content.SharedPreferences, WriteGuard>()
    @Synchronized fun forPreferences(preferences: android.content.SharedPreferences): WriteGuard =
        guards.getOrPut(preferences) { WriteGuard() }
}

internal fun androidCommit(editor: android.content.SharedPreferences.Editor): WriteResult =
    if (editor.commit()) WriteResult.SUCCESS else WriteResult.UNKNOWN
