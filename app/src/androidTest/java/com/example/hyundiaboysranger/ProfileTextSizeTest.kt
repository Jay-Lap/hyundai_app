package com.example.hyundiaboysranger

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileTextSizeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var originalSettings: UserSettings

    @Before
    fun saveOriginalSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        originalSettings = SettingsRepository(context).load()
    }

    @After
    fun restoreOriginalSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SettingsRepository(context).save(originalSettings)
    }

    @Test
    fun seniorPresetMakesTheVisibleHeadingLargerThanBeginnerPreset() {
        composeRule.onNodeWithText("사용자 유형").performClick()
        composeRule.onNode(hasClickAction() and hasText("초보자", substring = true)).performClick()
        val normal = composeRule.onNodeWithText("안전 운전 도우미").getUnclippedBoundsInRoot()
        val bodyText = "화면 시안 · 차량과 내비게이션이 연결되지 않았어요"
        val normalBody = composeRule.onNodeWithText(bodyText).getUnclippedBoundsInRoot()

        composeRule.onNode(hasClickAction() and hasText("고령자", substring = true)).performClick()
        val large = composeRule.onNodeWithText("안전 운전 도우미").getUnclippedBoundsInRoot()
        val largeBody = composeRule.onNodeWithText(bodyText).getUnclippedBoundsInRoot()

        assertTrue(
            "고령자 모드에서 제목의 실제 표시 높이가 커져야 합니다",
            (large.bottom - large.top) > (normal.bottom - normal.top),
        )
        assertTrue(
            "고령자 모드에서 안내 글자의 실제 표시 높이도 커져야 합니다",
            (largeBody.bottom - largeBody.top) > (normalBody.bottom - normalBody.top),
        )
    }
}
