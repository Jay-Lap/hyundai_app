package com.example.hyundiaboysranger

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewHistoryUiStateTest {
    @Test fun ambiguousHistoryRemainsStartedAndDoesNotClaimCompletion() {
        val state = ReviewHistoryUiState(false, null, "time", "source")
        assertEquals(R.string.review_history_started, reviewHistoryLabel(state))
        assertEquals(R.string.review_history_stopped, reviewHistoryLabel(state.copy(phase = "stop")))
        assertEquals(R.string.review_history_complete, reviewHistoryLabel(state.copy(complete = true)))
    }
}
