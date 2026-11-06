package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class VehicleHealthTest {
    @Test fun unknownAndContradictoryStatesNeverClaimSafeClosure() {
        assertTrue(DoorReport("문", DoorPosition.UNKNOWN, true).explanation.contains("확인하지 못"))
        assertTrue(DoorReport("문", DoorPosition.OPEN, true).explanation.contains("열림과 잠김"))
        assertFalse(DoorReport("문", DoorPosition.OPEN, null).explanation.contains("닫힘"))
        assertTrue(DoorReport("문", DoorPosition.CLOSED, null).explanation.contains("닫힘 신호"))
    }
}
