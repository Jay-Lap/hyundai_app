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

/** Each read fails closed. No synthetic P state or remembered permission is used. */
class ParkingMonitor(private val context: Context) {
    private var car: Car? = null

    suspend fun read(): ParkingEvidence = withContext(Dispatchers.IO) {
        try {
            val active = car ?: Car.createCar(context).also { car = it }
            if (active == null || !active.isConnected) return@withContext ParkingEvidence()
            val properties = active.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            val ux = active.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            val gear = properties.getProperty<Int>(VehiclePropertyIds.GEAR_SELECTION, 0)
            val speed = properties.getProperty<Float>(VehiclePropertyIds.PERF_VEHICLE_SPEED, 0)
            val restriction = ux.currentCarUxRestrictions
            ParkingEvidence(
                park = if (gear.status == CarPropertyValue.STATUS_AVAILABLE) gear.value == VehicleGear.GEAR_PARK else null,
                speed = if (speed.status == CarPropertyValue.STATUS_AVAILABLE) speed.value else null,
                unrestricted = restriction != null && !restriction.isRequiresDistractionOptimization && restriction.activeRestrictions == 0,
                observedAt = SystemClock.elapsedRealtime(),
            )
        } catch (_: Exception) { ParkingEvidence() }
    }

    fun close() { car?.disconnect(); car = null }
}
