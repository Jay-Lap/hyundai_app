package com.example.hyundiaboysranger

data class DoorReport(val name: String, val position: DoorPosition, val locked: Boolean?, val lockResult: SignalResult<Boolean> = locked?.let { SignalResult.Value(it) } ?: SignalResult.NoSignal) {
    val explanation: String get() = when {
        position == DoorPosition.OPEN && locked == true -> "열림과 잠김 신호가 함께 조회됐어요. 문 상태를 직접 확인해 주세요."
        position == DoorPosition.OPEN -> "문 열림 신호가 조회됐어요. 문 상태를 직접 확인해 주세요."
        position == DoorPosition.UNKNOWN -> "문 열림·닫힘 상태를 확인하지 못했어요."
        else -> "문 닫힘 신호가 조회됐어요."
    }
}

data class VehicleHealth(
    val doors: List<DoorReport> = emptyList(),
    val driverBeltWarning: Boolean? = null,
    val washerWarning: Boolean? = null,
    val beltResult: SignalResult<Boolean> = driverBeltWarning?.let { SignalResult.Value(it) } ?: SignalResult.NoSignal,
    val washerResult: SignalResult<Boolean> = washerWarning?.let { SignalResult.Value(it) } ?: SignalResult.NoSignal,
)
