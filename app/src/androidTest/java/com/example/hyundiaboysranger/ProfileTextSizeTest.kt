package com.example.hyundiaboysranger

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.hyundiaboysranger.ui.theme.HyundiaBoysRangerTheme
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileTextSizeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var originalSettings: UserSettings

    @Before
    fun saveOriginalSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        originalSettings = SettingsRepository(context).load()
        SettingsRepository(context).save(UserSettings())
        composeRule.setContent { HyundiaBoysRangerTheme { DrivingHelperApp() } }
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
        val bodyText = "주차 중 편하게 준비하고 연습해요"
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
    @Test
    fun sliderSupports200PercentAndProfileSwitchKeepsIt() {
        composeRule.onNodeWithText("설정").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("글자 크기 조절").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(200f) }
        composeRule.onNodeWithText("글자 크기 · 200%").assertExists()
        composeRule.onNodeWithText("홈으로").performScrollTo().performClick()
        composeRule.onNodeWithText("사용자 유형").performScrollTo().performClick()
        composeRule.onNode(hasClickAction() and hasText("고령자", substring = true)).performScrollTo().performClick()
        composeRule.onNodeWithText("글자 크기 따로 조절").performScrollTo().performClick()
        composeRule.onNodeWithText("글자 크기 · 200%").assertExists()
    }
}
