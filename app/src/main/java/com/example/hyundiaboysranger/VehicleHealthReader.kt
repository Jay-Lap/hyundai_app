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
import kotlin.coroutines.resume

class VehicleHealthReader(context: Context) : ChecklistDoorReader {
    private val vehicle = VehicleSdkAccess.create { Vehicle(context.applicationContext) }
    private fun readable(access: AccessType) = access == AccessType.READ || access == AccessType.READ_WRITE
    private fun failedHealth(failure: SignalResult<Nothing>) = VehicleHealth(
        doors = listOf("앞 왼쪽 문", "앞 오른쪽 문", "뒤 왼쪽 문", "뒤 오른쪽 문").map {
            DoorReport(it, DoorPosition.UNKNOWN, null, failure, failure)
        }, beltResult = failure, washerResult = failure,
    )

    private suspend fun <T> signal(block: ((SignalResult<T>) -> Unit) -> Unit): SignalResult<T> =
        withTimeoutOrNull(2500) {
            suspendCancellableCoroutine { continuation ->
                val finished = CallbackGate()
                continuation.invokeOnCancellation { finished.cancel() }
                val complete: (SignalResult<T>) -> Unit = { value -> if (finished.finish()) continuation.resume(value) }
                try { block(complete) } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    complete(signalFailure(error))
                }
            }
        } ?: SignalResult.Timeout

    suspend fun read(): VehicleHealth = withTimeoutOrNull(12_000) {
        val vehicle = vehicle ?: return@withTimeoutOrNull failedHealth(SignalResult.SafetyBlocked)
        try { vehicle.initialize() } catch (error: Exception) {
            if (error is CancellationException) throw error
            return@withTimeoutOrNull failedHealth(signalFailure(error))
        }
        coroutineScope {
            val doors = async { isolatedSignalRead({ readDoors() }) { failedHealth(it).doors } }
            val belt = async { isolatedSignalRead({ readBelt() }) { it } }
            val washer = async { isolatedSignalRead({ readWasher() }) { it } }
            val beltResult = belt.await()
            val washerResult = washer.await()
            VehicleHealth(doors.await(), beltResult.valueOrNull(), washerResult.valueOrNull(), beltResult, washerResult)
        }
    } ?: failedHealth(SignalResult.Timeout)

    /** Only checks and reads positions. It does not query locks or send any control command. */
    override suspend fun readChecklistDoors(): Map<ChecklistDoor, SignalResult<DoorPosition>> = withTimeoutOrNull(12_000) {
        val vehicle = vehicle ?: return@withTimeoutOrNull ChecklistDoor.entries.associateWith { SignalResult.SafetyBlocked }
        try {
            vehicle.initialize()
            val door = vehicle.getDoor()
            val capabilities = signal<Map<DoorArea, AreaBounds<Int>>> { done ->
                door.checkDoorPositionCapabilities(onComplete = { config, areas ->
                    done(if (readable(config.accessType)) SignalResult.Value(areas) else SignalResult.ReadNotAvailable)
                }, onFailed = { done(signalFailure(it)) })
            }
            val areas = listOf(
                ChecklistDoor.FRONT_LEFT to DoorValue.ROW_1_LEFT,
                ChecklistDoor.FRONT_RIGHT to DoorValue.ROW_1_RIGHT,
                ChecklistDoor.REAR_LEFT to DoorValue.ROW_2_LEFT,
                ChecklistDoor.REAR_RIGHT to DoorValue.ROW_2_RIGHT,
            )
            coroutineScope {
                areas.map { (key, value) -> async {
                    val area = DoorArea(value)
                    val result = capabilities.flatMap { bounds ->
                        val range = bounds[area] ?: return@flatMap SignalResult.AreaNotReported
                        val reading = signal<Int> { done ->
                            door.getCurrentDoorPosition(area,
                                onComplete = { done(if (it == null) SignalResult.NoSignal else SignalResult.Value(it)) },
                                onFailed = { done(signalFailure(it)) })
                        }
                        reading.flatMap { value ->
                            when (val position = DoorPosition.fromReading(value, range.minValue, range.maxValue)) {
                                DoorPosition.UNKNOWN -> SignalResult.NoSignal
                                else -> SignalResult.Value(position)
                            }
                        }
                    }
                    key to result
                } }.awaitAll().toMap()
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { ChecklistDoor.entries.associateWith { signalFailure(error) } }
    } ?: ChecklistDoor.entries.associateWith { SignalResult.Timeout }

    private suspend fun readDoors(): List<DoorReport> {
        val vehicle = requireNotNull(vehicle)
        val door = vehicle.getDoor()
        val bounds = signal<Map<DoorArea, AreaBounds<Int>>> { done ->
            door.checkDoorPositionCapabilities(onComplete = { config, areas ->
                done(if (readable(config.accessType)) SignalResult.Value(areas) else SignalResult.ReadNotAvailable)
            }, onFailed = { done(signalFailure(it)) })
        }
        val locks = signal<List<DoorArea>> { done ->
            door.checkDoorLockCapabilities(onComplete = { config, areas ->
                done(if (readable(config.accessType)) SignalResult.Value(areas) else SignalResult.ReadNotAvailable)
            }, onFailed = { done(signalFailure(it)) })
        }
        return coroutineScope {
            listOf("앞 왼쪽 문" to DoorValue.ROW_1_LEFT, "앞 오른쪽 문" to DoorValue.ROW_1_RIGHT,
                "뒤 왼쪽 문" to DoorValue.ROW_2_LEFT, "뒤 오른쪽 문" to DoorValue.ROW_2_RIGHT).map { (name, value) -> async {
                val area = DoorArea(value)
                val positionResult = isolatedSignalRead({ bounds.flatMap { areas ->
                    val range = areas[area] ?: return@flatMap SignalResult.AreaNotReported
                    val reading = signal<Int> { done ->
                        door.getCurrentDoorPosition(area,
                            onComplete = { done(if (it == null) SignalResult.NoSignal else SignalResult.Value(it)) },
                            onFailed = { done(signalFailure(it)) })
                    }
                    reading.flatMap { value ->
                        val position = DoorPosition.fromReading(value, range.minValue, range.maxValue)
                        if (position == DoorPosition.UNKNOWN) SignalResult.NoSignal else SignalResult.Value(position)
                    }
                } }) { it }
                val lockResult: SignalResult<Boolean> = isolatedSignalRead({ locks.flatMap { supportedAreas ->
                    if (area !in supportedAreas) SignalResult.AreaNotReported else signal { done ->
                        door.getDoorLockState(area, onComplete = { done(if (it == null) SignalResult.NoSignal else SignalResult.Value(it)) }, onFailed = { done(signalFailure(it)) })
                    }
                } }) { it }
                DoorReport(name, positionResult.valueOrNull() ?: DoorPosition.UNKNOWN, lockResult.valueOrNull(), lockResult, positionResult)
            } }.awaitAll()
        }
    }
    private suspend fun readBelt(): SignalResult<Boolean> {
        val vehicle = requireNotNull(vehicle)
        val safety = vehicle.getSafety()
        val area = SeatArea(SeatValue.ROW_1_LEFT)
        val supported = signal<Unit> { done ->
            safety.checkSeatNotBuckledWarningCapabilities(onComplete = { config, areas ->
                done(capabilityResult(readable(config.accessType), area in areas))
            }, onFailed = { done(signalFailure(it)) })
        }
        return supported.flatMap {
            signal { done ->
                safety.hasSeatBeltNotBuckledWarning(area, onComplete = { done(if (it == null) SignalResult.NoSignal else SignalResult.Value(it)) }, onFailed = { done(signalFailure(it)) })
            }
        }
    }
    private suspend fun readWasher(): SignalResult<Boolean> {
        val vehicle = requireNotNull(vehicle)
        val safety = vehicle.getSafety()
        val supported = signal<Unit> { done ->
            safety.checkLowWasherFluidWarningStateCapabilities(onComplete = { config -> done(capabilityResult(readable(config.accessType))) },
                onFailed = { done(signalFailure(it)) })
        }
        return supported.flatMap {
            signal { done ->
                safety.hasLowWasherFluidWarning(onComplete = { done(if (it == null) SignalResult.NoSignal else SignalResult.Value(it)) }, onFailed = { done(signalFailure(it)) })
            }
        }
    }
    override fun close() { try { vehicle?.release() } catch (_: Exception) { } }
}
