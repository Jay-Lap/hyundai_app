package com.example.hyundiaboysranger

internal const val DOOR_CHECK_INDEX = 2
internal fun displayedChecklistChecks(manual: Set<Int>, sdkConfirmed: Boolean): Set<Int> =
    if (sdkConfirmed) manual + DOOR_CHECK_INDEX else manual
internal fun updateManualCheck(manual: Set<Int>, index: Int, checked: Boolean): Set<Int> =
    if (checked) manual + index else manual - index

/** A complete set means all four distinct passenger-door areas, never just reported areas. */
enum class ChecklistDoor(val label: String) {
    FRONT_LEFT("앞 왼쪽 문"), FRONT_RIGHT("앞 오른쪽 문"),
    REAR_LEFT("뒤 왼쪽 문"), REAR_RIGHT("뒤 오른쪽 문")
}

interface ChecklistDoorReader {
    suspend fun readChecklistDoors(): Map<ChecklistDoor, SignalResult<DoorPosition>>
    fun close()
}

internal fun allChecklistDoorsClosed(results: Map<ChecklistDoor, SignalResult<DoorPosition>>): Boolean =
    results.keys == ChecklistDoor.entries.toSet() && results.values.all { it == SignalResult.Value(DoorPosition.CLOSED) }

data class DoorChecklistSnapshot(
    val results: Map<ChecklistDoor, SignalResult<DoorPosition>>,
    val requestedAt: Long,
    val displayedTime: Long,
)

data class DoorChecklistState(
    val loading: Boolean = false,
    val confirmed: Boolean = false,
    val snapshot: DoorChecklistSnapshot? = null,
)

/** Mutated on the UI thread; stale requests cannot establish an automatic check. */
class DoorChecklistController(private val maxAgeMs: Long = 15_000L) {
    var state: DoorChecklistState = DoorChecklistState(); private set
    private var generation = 0L
    private var active: Long? = null
    private var startedAt = -1L
    private var disposed = false

    fun begin(evidence: ParkingEvidence, now: Long): Long? {
        refresh(evidence, now)
        if (disposed || now < 0 || !FeatureGate.allowed(AppFeature.CHECKLIST, evidence, now)) return null
        generation++
        active = generation
        startedAt = now
        state = DoorChecklistState(loading = true)
        return generation
    }

    fun complete(token: Long, evidence: ParkingEvidence, now: Long, wallTime: Long,
        results: Map<ChecklistDoor, SignalResult<DoorPosition>>) {
        if (disposed || active != token || generation != token) return
        active = null
        val validGate = FeatureGate.allowed(AppFeature.CHECKLIST, evidence, now)
        val fresh = now - startedAt in 0..maxAgeMs
        val allClosed = allChecklistDoorsClosed(results)
        state = DoorChecklistState(confirmed = validGate && fresh && allClosed,
            snapshot = DoorChecklistSnapshot(results.toMap(), startedAt, wallTime))
    }

    fun refresh(evidence: ParkingEvidence, now: Long) {
        val gate = FeatureGate.allowed(AppFeature.CHECKLIST, evidence, now)
        val requestFresh = now - startedAt in 0..maxAgeMs
        if (!gate || ((state.loading || state.confirmed) && !requestFresh)) {
            if (active != null || state.confirmed) generation++
            active = null
            state = state.copy(loading = false, confirmed = false)
        }
    }

    /** Unchecking or resetting also invalidates a response already in flight. */
    fun clear(clearSnapshot: Boolean = false) {
        generation++
        active = null
        state = if (clearSnapshot) DoorChecklistState() else state.copy(loading = false, confirmed = false)
    }
    fun close() { clear(clearSnapshot = true); disposed = true }
}
