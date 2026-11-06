package com.example.hyundiaboysranger

import android.content.Context

/** Keep optional android.car types out of the entry point's fields and signatures. */
class ParkingMonitor(context: Context) {
    private val reader: (suspend () -> ParkingEvidence)?
    private val cleanup: (() -> Unit)?

    init {
        var read: (suspend () -> ParkingEvidence)? = null
        var close: (() -> Unit)? = null
        try {
            Class.forName("android.car.Car", false, context.classLoader)
            val monitor = AndroidCarParkingMonitor(context)
            read = { monitor.read() }
            close = { monitor.close() }
        } catch (_: ClassNotFoundException) {
            // Pleos images may not expose Android Automotive's shared library.
        } catch (_: LinkageError) {
            // A partial/incompatible optional library must also fail closed.
        }
        reader = read
        cleanup = close
    }

    val available: Boolean get() = reader != null

    suspend fun read(): ParkingEvidence = reader?.invoke() ?: ParkingEvidence()
    fun close() { cleanup?.invoke() }
}
