package com.example.hyundiaboysranger

import java.util.concurrent.atomic.AtomicBoolean

/** One-shot completion; cancellation and timeouts make later callbacks inert. */
internal class CallbackGate {
    private val finished = AtomicBoolean(false)
    fun active(): Boolean = !finished.get()
    fun finish(): Boolean = finished.compareAndSet(false, true)
    fun cancel() { finished.set(true) }
}
