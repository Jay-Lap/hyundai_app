package com.example.hyundiaboysranger

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

/** Each read fails closed. No synthetic P state or remembered permission is used. */
internal class AndroidCarParkingMonitor(private val context: Context) {
    private var car: Car? = null
    private var connectionStartedAt = 0L
    private var wasConnected = false

    suspend fun read(): ParkingEvidence = withContext(Dispatchers.IO) {
        try {
            val active = car ?: Car.createCar(context).also {
                car = it
                connectionStartedAt = SystemClock.elapsedRealtime()
                wasConnected = false
            }
            if (active == null) return@withContext ParkingEvidence()
            if (!active.isConnected) {
                if (wasConnected || SystemClock.elapsedRealtime() - connectionStartedAt > 5_000) {
                    try { active.disconnect() } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { }
                    car = null
                    wasConnected = false
                }
                return@withContext ParkingEvidence()
            }
            wasConnected = true
            val pollStartedAt = SystemClock.elapsedRealtime()
            val properties = active.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            val ux = active.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            val gear = properties.getProperty<Int>(VehiclePropertyIds.GEAR_SELECTION, 0)
            val speed = properties.getProperty<Float>(VehiclePropertyIds.PERF_VEHICLE_SPEED, 0)
            val restriction = ux.currentCarUxRestrictions
            parkingEvidenceFromPoll(
                connectedAfter = active.isConnected,
                startedAt = pollStartedAt,
                completedAt = SystemClock.elapsedRealtime(),
                park = if (gear.status == CarPropertyValue.STATUS_AVAILABLE) gear.value == VehicleGear.GEAR_PARK else null,
                speed = if (speed.status == CarPropertyValue.STATUS_AVAILABLE) speed.value else null,
                unrestricted = restriction != null && !restriction.isRequiresDistractionOptimization && restriction.activeRestrictions == 0,
            )
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: LinkageError) { ParkingEvidence() }
        catch (_: Exception) { ParkingEvidence() }
    }

    fun close() {
        try { car?.disconnect() } catch (_: LinkageError) {
            // Optional platform library unavailable.
        } catch (_: Exception) {
            // Service already disconnected.
        } finally { car = null; wasConnected = false }
    }
}
