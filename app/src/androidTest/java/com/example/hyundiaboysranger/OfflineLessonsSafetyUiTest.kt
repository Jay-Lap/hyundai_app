package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/** No Vehicle SDK or user storage is accessed. */
class OfflineLessonsSafetyUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun climateAndDefogCanBeReadButNotCompleted() {
        rule.setContent { Column { ExtraLesson.entries.forEach { ExtraLessonGuide(it) } } }
        rule.onNodeWithText("공조 조작 안내 보기").performClick()
        rule.onNodeWithText("공조 전원과 온도", substring = true).assertExists()
        rule.onNodeWithText("완료 판정 사용 불가").assertIsNotEnabled()
        rule.onNodeWithText("공조 조작 안내 닫기").performClick()
        rule.onNodeWithText("김서림 제거 안내 보기").performClick()
        rule.onNodeWithText("앞유리 김서림 제거", substring = true).assertExists()
        rule.onNodeWithText("완료 판정 사용 불가").assertIsNotEnabled()
    }
}
