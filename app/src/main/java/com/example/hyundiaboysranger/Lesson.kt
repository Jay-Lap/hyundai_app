package com.example.hyundiaboysranger

/** A lesson receives observed signals only; it never controls vehicle hardware. */
interface Lesson<S, T> { fun observe(state: S, signal: T?): S }

object WiperLesson : Lesson<PracticeProgress, Boolean> {
    override fun observe(state: PracticeProgress, signal: Boolean?): PracticeProgress = state.observe(signal)
}

enum class ExtraLesson(val titleResource: Int, val explanationResource: Int) {
    CLIMATE(R.string.lesson_climate, R.string.lesson_climate_guidance),
    DEFOG(R.string.lesson_defog, R.string.lesson_defog_guidance)
}

/** Informational only until an actual vehicle signal is verified for the lesson. */
data class ExtraLessonUiState(val expanded: Boolean = false) {
    val canConfirmCompletion: Boolean get() = false
}
sealed interface ExtraLessonUiEvent { data object Toggle : ExtraLessonUiEvent }
fun reduceExtraLesson(state: ExtraLessonUiState, event: ExtraLessonUiEvent): ExtraLessonUiState = when (event) {
    ExtraLessonUiEvent.Toggle -> state.copy(expanded = !state.expanded)
}
