package com.example.hyundiaboysranger

/** Unknown causes are not inferred from the execution environment. */
sealed interface SignalResult<out T> {
    data class Value<T>(val value: T) : SignalResult<T>
    data object NoSignal : SignalResult<Nothing>
    data object PermissionDenied : SignalResult<Nothing>
    data object Timeout : SignalResult<Nothing>
    data object EmulatorUnavailable : SignalResult<Nothing>
    data object Error : SignalResult<Nothing>
}

fun signalFailure(error: Throwable): SignalResult<Nothing> =
    if (error is SecurityException) SignalResult.PermissionDenied else SignalResult.Error

fun <T> SignalResult<T>.valueOrNull(): T? = (this as? SignalResult.Value<T>)?.value

/** Interactions fail closed for missing, stale, restricted, or invalid evidence. */
enum class AppFeature { PRACTICE, SETTINGS, CHECKLIST, ROUTE, REVIEW, VEHICLE_STATUS }
object FeatureGate {
    fun allowed(feature: AppFeature, evidence: ParkingEvidence, now: Long): Boolean = when (feature) {
        AppFeature.PRACTICE, AppFeature.SETTINGS, AppFeature.CHECKLIST, AppFeature.ROUTE,
        AppFeature.REVIEW, AppFeature.VEHICLE_STATUS -> evidence.allowsPractice(now)
    }
}
