package com.example.hyundiaboysranger

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.DoorValue
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Reads the driver's door only; this class never issues a vehicle control command. */
class VehicleDoorReader(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val disposed = AtomicBoolean(false)
    private val latestRequest = AtomicInteger(0)
    private var vehicle: Vehicle? = null

    private fun failureMessage(error: Throwable): String = appContext.getString(when (signalFailure(error)) {
        SignalResult.PermissionDenied -> R.string.signal_permission
        SignalResult.Unsupported -> R.string.signal_unsupported
        SignalResult.Restricted -> R.string.signal_restricted
        SignalResult.ServiceUnavailable -> R.string.signal_service_unavailable
        SignalResult.NoSignal -> R.string.signal_no_data
        else -> R.string.signal_error
    })

    fun read(onResult: (String) -> Unit) {
        if (disposed.get()) return
        if (!VehicleSdkAccess.enabled) { onResult(appContext.getString(R.string.vehicle_safety_blocked)); return }
        val requestId = latestRequest.incrementAndGet()
        val completed = AtomicBoolean(false)

        fun finish(message: String) {
            if (!completed.compareAndSet(false, true)) return
            mainHandler.post {
                if (!disposed.get() && latestRequest.get() == requestId) onResult(message)
            }
        }

        onResult("문 상태를 확인하고 있어요.")
        mainHandler.postDelayed({ finish("응답이 늦어 확인하지 못했어요. 다시 조회해 주세요.") }, 10_000)

        try {
            val activeVehicle = vehicle ?: (VehicleSdkAccess.create { Vehicle(appContext) } ?: return).also {
                try {
                    it.initialize()
                } catch (error: Exception) {
                    try {
                        it.release()
                    } catch (_: Exception) {
                        // Initialization can fail before the service is bound.
                    }
                    throw error
                }
                vehicle = it
            }
            val door = activeVehicle.getDoor()
            val area = DoorArea(DoorValue.ROW_1_LEFT)
            door.checkDoorPositionCapabilities(
                onComplete = { config, areaBounds ->
                    if (!disposed.get() && !completed.get() && latestRequest.get() == requestId) {
                        val bounds = areaBounds[area]
                        if (config.accessType != AccessType.READ && config.accessType != AccessType.READ_WRITE) {
                            finish(appContext.getString(R.string.signal_read_unavailable))
                        } else if (bounds == null) {
                            finish(appContext.getString(R.string.signal_area_not_reported))
                        } else {
                            try {
                                door.getCurrentDoorPosition(
                                    area = area,
                                    onComplete = { value ->
                                        val message = when (DoorPosition.fromReading(value, bounds.minValue, bounds.maxValue)) {
                                            DoorPosition.OPEN -> "마지막 조회: 운전석 문이 열려 있어요."
                                            DoorPosition.CLOSED -> "마지막 조회: 운전석 문이 닫혀 있어요."
                                            DoorPosition.UNKNOWN -> "받은 문 상태를 해석할 수 없어요."
                                        }
                                        finish(message)
                                    },
                                    onFailed = { finish(failureMessage(it)) },
                                )
                            } catch (error: Exception) {
                                finish(failureMessage(error))
                            }
                        }
                    }
                },
                onFailed = { finish(failureMessage(it)) },
            )
        } catch (error: Exception) {
            finish(failureMessage(error))
        }
    }

    fun close() {
        if (!disposed.compareAndSet(false, true)) return
        latestRequest.incrementAndGet()
        try {
            vehicle?.release()
        } catch (_: Exception) {
            // The service may already have disconnected.
        } finally {
            vehicle = null
        }
    }
}
