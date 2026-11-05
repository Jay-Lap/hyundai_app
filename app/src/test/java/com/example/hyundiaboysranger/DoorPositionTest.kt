package com.example.hyundiaboysranger

import org.junit.Assert.assertEquals
import org.junit.Test

class DoorPositionTest {
    @Test
    fun interpretsBinaryDoorValuesOnlyWhenTheReportedRangeMatches() {
        assertEquals(DoorPosition.CLOSED, DoorPosition.fromReading(0, 0, 1))
        assertEquals(DoorPosition.OPEN, DoorPosition.fromReading(1, 0, 1))
        assertEquals(DoorPosition.UNKNOWN, DoorPosition.fromReading(null, 0, 1))
        assertEquals(DoorPosition.UNKNOWN, DoorPosition.fromReading(2, 0, 1))
        assertEquals(DoorPosition.UNKNOWN, DoorPosition.fromReading(0, null, null))
        assertEquals(DoorPosition.UNKNOWN, DoorPosition.fromReading(1, 0, 100))
    }
}
