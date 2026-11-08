package com.example.hyundiaboysranger

import kotlinx.coroutines.CancellationException

/** Unknown causes are not inferred from the execution environment. */
sealed interface SignalResult<out T> {
    data class Value<T>(val value: T) : SignalResult<T>
    data object NoSignal : SignalResult<Nothing>
    data object ReadNotAvailable : SignalResult<Nothing>
    data object AreaNotReported : SignalResult<Nothing>
    data object PermissionDenied : SignalResult<Nothing>
    data object Unsupported : SignalResult<Nothing>
    data object Restricted : SignalResult<Nothing>
    data object ServiceUnavailable : SignalResult<Nothing>
    data object SafetyBlocked : SignalResult<Nothing>
    data object Timeout : SignalResult<Nothing>
    data object EmulatorUnavailable : SignalResult<Nothing>
    data object Error : SignalResult<Nothing>
}

fun signalFailure(error: Throwable): SignalResult<Nothing> = when (error) {
    is SecurityException -> SignalResult.PermissionDenied
    else -> SignalResult.Error
}

/** A single failed domain must not discard independently obtained results. */
internal suspend fun <T> isolatedSignalRead(read: suspend () -> T, failed: (SignalResult<Nothing>) -> T): T = try {
    read()
} catch (cancelled: CancellationException) { throw cancelled }
catch (error: Exception) { failed(signalFailure(error)) }
catch (_: LinkageError) { failed(SignalResult.Error) }

fun <T> SignalResult<T>.valueOrNull(): T? = (this as? SignalResult.Value<T>)?.value

inline fun <T, R> SignalResult<T>.flatMap(transform: (T) -> SignalResult<R>): SignalResult<R> = when (this) {
    is SignalResult.Value -> transform(value)
    SignalResult.NoSignal -> SignalResult.NoSignal
    SignalResult.ReadNotAvailable -> SignalResult.ReadNotAvailable
    SignalResult.AreaNotReported -> SignalResult.AreaNotReported
    SignalResult.PermissionDenied -> SignalResult.PermissionDenied
    SignalResult.Unsupported -> SignalResult.Unsupported
    SignalResult.Restricted -> SignalResult.Restricted
    SignalResult.ServiceUnavailable -> SignalResult.ServiceUnavailable
    SignalResult.SafetyBlocked -> SignalResult.SafetyBlocked
    SignalResult.Timeout -> SignalResult.Timeout
    SignalResult.EmulatorUnavailable -> SignalResult.EmulatorUnavailable
    SignalResult.Error -> SignalResult.Error
}

fun capabilityResult(readable: Boolean, areaReported: Boolean = true): SignalResult<Unit> = when {
    !readable -> SignalResult.ReadNotAvailable
    !areaReported -> SignalResult.AreaNotReported
    else -> SignalResult.Value(Unit)
}

/** Interactions fail closed for missing, stale, restricted, or invalid evidence. */
enum class AppFeature { PRACTICE, SETTINGS, CHECKLIST, ROUTE, REVIEW, VEHICLE_STATUS }
object FeatureGate {
    fun allowed(feature: AppFeature, evidence: ParkingEvidence, now: Long): Boolean = when (feature) {
        AppFeature.PRACTICE, AppFeature.SETTINGS, AppFeature.CHECKLIST, AppFeature.ROUTE,
        AppFeature.REVIEW, AppFeature.VEHICLE_STATUS -> evidence.allowsPractice(now)
    }
}
