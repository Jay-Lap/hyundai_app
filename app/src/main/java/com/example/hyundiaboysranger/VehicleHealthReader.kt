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

    private suspend fun <T> signal(block: ((SignalResult<T>) -> Unit) -> Unit): SignalResult<T> =
        withTimeoutOrNull(2500) {
            suspendCancellableCoroutine { continuation ->
                val finished = AtomicBoolean(false)
                continuation.invokeOnCancellation { finished.set(true) }
                val complete: (SignalResult<T>) -> Unit = { value -> if (finished.compareAndSet(false, true)) continuation.resume(value) }
                try { block(complete) } catch (error: Exception) { complete(signalFailure(error)) }
            }
        } ?: SignalResult.Timeout

    private suspend fun <T> result(block: ((T?) -> Unit) -> Unit): T? =
        signal<T> { done -> block { value -> done(if (value == null) SignalResult.NoSignal else SignalResult.Value(value)) } }.valueOrNull()

    suspend fun read(): VehicleHealth = withTimeoutOrNull(12_000) {
        try { vehicle.initialize() } catch (error: Exception) { val failure = signalFailure(error); return@withTimeoutOrNull VehicleHealth(beltResult = failure, washerResult = failure) }
        coroutineScope {
            val doors = async { readDoors() }
            val belt = async { readBelt() }
            val washer = async { readWasher() }
            val beltResult = belt.await()
            val washerResult = washer.await()
            VehicleHealth(doors.await(), beltResult.valueOrNull(), washerResult.valueOrNull(), beltResult, washerResult)
        }
    } ?: VehicleHealth(beltResult = SignalResult.Timeout, washerResult = SignalResult.Timeout)

    private suspend fun readDoors(): List<DoorReport> {
        val door = vehicle.getDoor()
        val bounds = result<Map<DoorArea, AreaBounds<Int>>> { done ->
            door.checkDoorPositionCapabilities(onComplete = { config, areas -> done(if (readable(config.accessType)) areas else null) }, onFailed = { done(null) })
        }.orEmpty()
        val locks = signal<List<DoorArea>> { done ->
            door.checkDoorLockCapabilities(onComplete = { config, areas ->
                done(if (readable(config.accessType)) SignalResult.Value(areas) else SignalResult.NoSignal)
            }, onFailed = { done(signalFailure(it)) })
        }
        return coroutineScope {
            listOf("앞 왼쪽 문" to DoorValue.ROW_1_LEFT, "앞 오른쪽 문" to DoorValue.ROW_1_RIGHT,
                "뒤 왼쪽 문" to DoorValue.ROW_2_LEFT, "뒤 오른쪽 문" to DoorValue.ROW_2_RIGHT).map { (name, value) -> async {
                val area = DoorArea(value)
                val range = bounds[area]
                val position = if (range == null) DoorPosition.UNKNOWN else {
                    val reading = result<Int> { done -> door.getCurrentDoorPosition(area, onComplete = { done(it) }, onFailed = { done(null) }) }
                    DoorPosition.fromReading(reading, range.minValue, range.maxValue)
                }
                val lockResult: SignalResult<Boolean> = when (locks) {
                    is SignalResult.Value -> if (area !in locks.value) SignalResult.NoSignal else signal { done ->
                        door.getDoorLockState(area, onComplete = { done(if (it == null) SignalResult.NoSignal else SignalResult.Value(it)) }, onFailed = { done(signalFailure(it)) })
                    }
                    SignalResult.NoSignal -> SignalResult.NoSignal
                    SignalResult.PermissionDenied -> SignalResult.PermissionDenied
                    SignalResult.Timeout -> SignalResult.Timeout
                    SignalResult.EmulatorUnavailable -> SignalResult.EmulatorUnavailable
                    SignalResult.Error -> SignalResult.Error
                }
                DoorReport(name, position, lockResult.valueOrNull(), lockResult)
            } }.awaitAll()
        }
    }
    private suspend fun readBelt(): SignalResult<Boolean> {
        val safety = vehicle.getSafety()
        val area = SeatArea(SeatValue.ROW_1_LEFT)
        val supported = signal<Boolean> { done ->
            safety.checkSeatNotBuckledWarningCapabilities(onComplete = { config, areas ->
                done(SignalResult.Value(readable(config.accessType) && area in areas))
            }, onFailed = { done(signalFailure(it)) })
        }
        return when (supported) {
            is SignalResult.Value -> if (!supported.value) SignalResult.NoSignal else signal { done ->
                safety.hasSeatBeltNotBuckledWarning(area, onComplete = { done(if (it == null) SignalResult.NoSignal else SignalResult.Value(it)) }, onFailed = { done(signalFailure(it)) })
            }
            SignalResult.NoSignal -> SignalResult.NoSignal
            SignalResult.PermissionDenied -> SignalResult.PermissionDenied
            SignalResult.Timeout -> SignalResult.Timeout
            SignalResult.EmulatorUnavailable -> SignalResult.EmulatorUnavailable
            SignalResult.Error -> SignalResult.Error
        }
    }
    private suspend fun readWasher(): SignalResult<Boolean> {
        val safety = vehicle.getSafety()
        val supported = signal<Boolean> { done ->
            safety.checkLowWasherFluidWarningStateCapabilities(onComplete = { config -> done(SignalResult.Value(readable(config.accessType))) },
                onFailed = { done(signalFailure(it)) })
        }
        return when (supported) {
            is SignalResult.Value -> if (!supported.value) SignalResult.NoSignal else signal { done ->
                safety.hasLowWasherFluidWarning(onComplete = { done(if (it == null) SignalResult.NoSignal else SignalResult.Value(it)) }, onFailed = { done(signalFailure(it)) })
            }
            SignalResult.NoSignal -> SignalResult.NoSignal
            SignalResult.PermissionDenied -> SignalResult.PermissionDenied
            SignalResult.Timeout -> SignalResult.Timeout
            SignalResult.EmulatorUnavailable -> SignalResult.EmulatorUnavailable
            SignalResult.Error -> SignalResult.Error
        }
    }
    fun close() { try { vehicle.release() } catch (_: Exception) { } }
}
