package com.example.hyundiaboysranger

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.WiperState
import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.resume

/** Read-only SDK adapter; cancellation prevents late callbacks from completing a practice. */
class WiperReader(context: Context) {
    private val appContext = context.applicationContext
    private var vehicle: Vehicle? = null
    suspend fun read(): SignalResult<Boolean> = withContext(Dispatchers.IO) { withTimeoutOrNull<SignalResult<Boolean>>(2000) {
        if (!VehicleSdkAccess.enabled) return@withTimeoutOrNull SignalResult.SafetyBlocked
        suspendCancellableCoroutine { continuation ->
            val finished = CallbackGate()
            continuation.invokeOnCancellation { finished.cancel() }
            fun finish(value: SignalResult<Boolean>) {
                if (finished.finish()) continuation.resume(value)
            }
            try {
                val active = vehicle ?: (VehicleSdkAccess.create { Vehicle(appContext) } ?: run { finish(SignalResult.SafetyBlocked); return@suspendCancellableCoroutine }).also {
                    try { it.initialize() } catch (error: Exception) {
                        try { it.release() } catch (_: Exception) { }
                        throw error
                    }
                    vehicle = it
                }
                val wiper = active.getWiper()
                val area = WindowArea(WindowValue.FRONT_WINDSHIELD)
                wiper.checkWiperStateCapabilities(
                    onComplete = { config, areas ->
                        if (finished.active()) {
                            val states = areas[area].orEmpty()
                            if (config.accessType !in listOf(AccessType.READ, AccessType.READ_WRITE)) finish(SignalResult.ReadNotAvailable)
                            else if (!areas.containsKey(area)) finish(SignalResult.AreaNotReported)
                            else if (!states.containsAll(listOf(WiperState.OFF, WiperState.ON))) finish(SignalResult.NoSignal)
                            else try {
                                wiper.getCurrentWiperState(area,
                                    onComplete = { state -> finish(when (state) {
                                        WiperState.ON -> SignalResult.Value(true)
                                        WiperState.OFF -> SignalResult.Value(false)
                                        else -> SignalResult.NoSignal
                                    }) }, onFailed = { finish(signalFailure(it)) })
                            } catch (error: Exception) {
                                if (error is CancellationException) throw error
                                finish(signalFailure(error))
                            }
                        }
                    }, onFailed = { finish(signalFailure(it)) })
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                finish(signalFailure(error))
            }
        }
    } ?: SignalResult.Timeout }
    fun close() {
        try { vehicle?.release() } catch (_: Exception) { /* disconnected */ }
        vehicle = null
    }
}
