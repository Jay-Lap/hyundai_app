package com.example.hyundiaboysranger

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.WiperState
import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/** Read-only SDK adapter; cancellation prevents late callbacks from completing a practice. */
class WiperReader(context: Context) {
    private val appContext = context.applicationContext
    private var vehicle: Vehicle? = null
    suspend fun read(): Boolean? = withTimeoutOrNull(2000) {
        suspendCancellableCoroutine { continuation ->
            val finished = AtomicBoolean(false)
            continuation.invokeOnCancellation { finished.set(true) }
            fun finish(value: Boolean?) {
                if (finished.compareAndSet(false, true)) continuation.resume(value)
            }
            try {
                val active = vehicle ?: Vehicle(appContext).also {
                    vehicle = it
                    it.initialize()
                }
                val wiper = active.getWiper()
                val area = WindowArea(WindowValue.FRONT_WINDSHIELD)
                wiper.checkWiperStateCapabilities(
                    onComplete = { config, areas ->
                        if (!finished.get()) {
                            val states = areas[area].orEmpty()
                            if (config.accessType !in listOf(AccessType.READ, AccessType.READ_WRITE) ||
                                !states.containsAll(listOf(WiperState.OFF, WiperState.ON))) finish(null)
                            else try {
                                wiper.getCurrentWiperState(area,
                                    onComplete = { state -> finish(when (state) {
                                        WiperState.ON -> true
                                        WiperState.OFF -> false
                                        else -> null
                                    }) }, onFailed = { finish(null) })
                            } catch (_: Exception) { finish(null) }
                        }
                    }, onFailed = { finish(null) })
            } catch (_: Exception) { finish(null) }
        }
    }
    fun close() {
        try { vehicle?.release() } catch (_: Exception) { /* disconnected */ }
        vehicle = null
    }
}
