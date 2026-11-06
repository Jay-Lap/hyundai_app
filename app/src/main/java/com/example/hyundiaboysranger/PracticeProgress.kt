package com.example.hyundiaboysranger

/** Pure state machine: an existing ON reading never counts as a completed exercise. */
enum class PracticeProgress(val label: String) {
    READY("연습 시작 전"), WAIT_OFF("먼저 와이퍼를 꺼 주세요"),
    WAIT_ON("와이퍼를 켜 주세요"), WAIT_FINAL_OFF("작동을 확인했어요. 다시 꺼 주세요"),
    COMPLETE("켜기와 끄기를 모두 확인했어요");

    fun observe(on: Boolean?): PracticeProgress = when {
        on == null && this != COMPLETE && this != READY -> WAIT_OFF
        this == WAIT_OFF && on == false -> WAIT_ON
        this == WAIT_ON && on == true -> WAIT_FINAL_OFF
        this == WAIT_FINAL_OFF && on == false -> COMPLETE
        else -> this
    }
}

data class ParkingEvidence(
    val park: Boolean? = null,
    val speed: Float? = null,
    val unrestricted: Boolean? = null,
    val observedAt: Long = -1,
) {
    fun allowsPractice(now: Long): Boolean = park == true && speed != null &&
        speed.isFinite() && kotlin.math.abs(speed) <= 0.01f && unrestricted == true &&
        observedAt >= 0 && now - observedAt in 0..2500
}
