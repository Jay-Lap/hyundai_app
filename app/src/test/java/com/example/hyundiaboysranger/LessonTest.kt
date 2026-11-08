package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class LessonTest {
    @Test fun unverifiedLessonCanShowGuidanceButNeverMarksCompletion() {
        val initial = ExtraLessonUiState()
        val expanded = reduceExtraLesson(initial, ExtraLessonUiEvent.Toggle)
        assertTrue(expanded.expanded); assertFalse(expanded.canConfirmCompletion)
        val closed = reduceExtraLesson(expanded, ExtraLessonUiEvent.Toggle)
        assertFalse(closed.expanded); assertFalse(closed.canConfirmCompletion)
    }
    @Test fun wiperAdapterPreservesExistingCompletionRules() {
        var state = PracticeProgress.WAIT_OFF
        state = WiperLesson.observe(state, false)
        state = WiperLesson.observe(state, true)
        assertEquals(PracticeProgress.WAIT_FINAL_OFF, state)
        assertEquals(PracticeProgress.COMPLETE, WiperLesson.observe(state, false))
        assertEquals(PracticeProgress.WAIT_OFF, WiperLesson.observe(state, null))
    }
}
