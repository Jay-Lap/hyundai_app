package com.example.hyundiaboysranger

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import android.content.Context
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class VehicleHealthReader(context: Context) {
    private val vehicle = Vehicle(context.applicationContext)
    private fun readable(access: AccessType) = access == AccessType.READ || access == AccessType.READ_WRITE

    private suspend fun <T> result(block: ((T?) -> Unit) -> Unit): T? = withTimeoutOrNull(2500) {
        suspendCancellableCoroutine { continuation ->
            val finished = AtomicBoolean(false)
            continuation.invokeOnCancellation { finished.set(true) }
            val complete: (T?) -> Unit = { value -> if (finished.compareAndSet(false, true)) continuation.resume(value) }
            try { block(complete) } catch (_: Exception) { complete(null) }
        }
    }

    suspend fun read(): VehicleHealth = withTimeoutOrNull(12_000) {
        try { vehicle.initialize() } catch (_: Exception) { return@withTimeoutOrNull VehicleHealth() }
        coroutineScope {
            val doors = async { readDoors() }
            val belt = async { readBelt() }
            val washer = async { readWasher() }
            VehicleHealth(doors.await(), belt.await(), washer.await())
        }
    } ?: VehicleHealth()

    private suspend fun readDoors(): List<DoorReport> {
        val door = vehicle.getDoor()
        val bounds = result<Map<DoorArea, AreaBounds<Int>>> { done ->
            door.checkDoorPositionCapabilities(onComplete = { config, areas -> done(if (readable(config.accessType)) areas else null) }, onFailed = { done(null) })
        }.orEmpty()
        val locks = result<List<DoorArea>> { done ->
            door.checkDoorLockCapabilities(onComplete = { config, areas -> done(if (readable(config.accessType)) areas else null) }, onFailed = { done(null) })
        }.orEmpty()
        return coroutineScope {
            listOf("앞 왼쪽 문" to DoorValue.ROW_1_LEFT, "앞 오른쪽 문" to DoorValue.ROW_1_RIGHT,
                "뒤 왼쪽 문" to DoorValue.ROW_2_LEFT, "뒤 오른쪽 문" to DoorValue.ROW_2_RIGHT).map { (name, value) -> async {
                val area = DoorArea(value)
                val range = bounds[area]
                val position = if (range == null) DoorPosition.UNKNOWN else {
                    val reading = result<Int> { done -> door.getCurrentDoorPosition(area, onComplete = { done(it) }, onFailed = { done(null) }) }
                    DoorPosition.fromReading(reading, range.minValue, range.maxValue)
                }
                val locked = if (area !in locks) null else result<Boolean> { done ->
                    door.getDoorLockState(area, onComplete = { done(it) }, onFailed = { done(null) })
                }
                DoorReport(name, position, locked)
            } }.awaitAll()
        }
    }
    private suspend fun readBelt(): Boolean? {
        val safety = vehicle.getSafety()
        val area = SeatArea(SeatValue.ROW_1_LEFT)
        val supported = result<Boolean> { done ->
            safety.checkSeatNotBuckledWarningCapabilities(onComplete = { config, areas -> done(readable(config.accessType) && area in areas) }, onFailed = { done(null) })
        }
        if (supported != true) return null
        return result { done -> safety.hasSeatBeltNotBuckledWarning(area, onComplete = { done(it) }, onFailed = { done(null) }) }
    }
    private suspend fun readWasher(): Boolean? {
        val safety = vehicle.getSafety()
        val supported = result<Boolean> { done ->
            safety.checkLowWasherFluidWarningStateCapabilities(onComplete = { config -> done(readable(config.accessType)) }, onFailed = { done(null) })
        }
        if (supported != true) return null
        return result { done -> safety.hasLowWasherFluidWarning(onComplete = { done(it) }, onFailed = { done(null) }) }
    }
    fun close() { try { vehicle.release() } catch (_: Exception) { } }
}
