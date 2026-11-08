package com.example.hyundiaboysranger

/** Compile-time safety stop. Changing this requires the documented lifecycle review. */
internal object VehicleSdkAccess {
    const val enabled = false
    inline fun <T> create(factory: () -> T): T? = if (enabled) factory() else null
}
