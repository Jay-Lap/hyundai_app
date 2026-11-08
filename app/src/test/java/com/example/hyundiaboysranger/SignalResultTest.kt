package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class SignalResultTest {
    @Test fun failedCapabilityDoesNotReadAndPreservesCause() {
        var queried = false
        val capability: SignalResult<Int> = SignalResult.PermissionDenied
        val result = capability.flatMap { queried = true; SignalResult.Value(it + 1) }
        assertFalse(queried)
        assertEquals(SignalResult.PermissionDenied, result)
        assertNull(result.valueOrNull())
    }

    @Test fun successfulCapabilityCanReturnMissingArea() {
        val capability = SignalResult.Value(emptyMap<String, Int>())
        val result = capability.flatMap { it["front"]?.let { value -> SignalResult.Value(value) } ?: SignalResult.AreaNotReported }
        assertEquals(SignalResult.AreaNotReported, result)
    }
}
