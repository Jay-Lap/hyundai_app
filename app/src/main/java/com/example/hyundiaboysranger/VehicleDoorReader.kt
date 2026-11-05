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

    fun read(onResult: (String) -> Unit) {
        if (disposed.get()) return
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
            val activeVehicle = vehicle ?: Vehicle(appContext).also {
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
                        if ((config.accessType != AccessType.READ && config.accessType != AccessType.READ_WRITE) || bounds == null) {
                            finish("이 차량에서 운전석 문 상태 조회를 지원하는지 확인할 수 없어요.")
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
                                    onFailed = { finish("문 상태를 확인하지 못했어요. 차량 연결과 권한을 확인해 주세요.") },
                                )
                            } catch (_: Exception) {
                                finish("문 상태를 확인하지 못했어요. 차량 연결과 권한을 확인해 주세요.")
                            }
                        }
                    }
                },
                onFailed = { finish("문 상태 조회 지원 여부를 확인하지 못했어요.") },
            )
        } catch (_: Exception) {
            finish("차량 서비스에 연결하지 못했어요. 다시 조회해 주세요.")
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
