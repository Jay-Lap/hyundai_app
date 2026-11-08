package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Only in-memory values and callbacks; no user settings or review store. */
class AppearanceReviewDialogUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun contrastAndModeCanBeReachedAtDoubleSize() {
        val events = mutableListOf<AppearanceEvent>()
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.size(280.dp, 220.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) { SettingsAppearanceControls(UserSettings()) { events.add(it) } }
                }
            }
        }
        rule.onNodeWithContentDescription("높은 대비").performScrollTo().performClick()
        rule.onNodeWithText("야간").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(listOf(AppearanceEvent.Contrast(true), AppearanceEvent.Mode(DisplayMode.NIGHT)), events) }
    }

    @Test fun deleteDialogCancelAndConfirmEmitDistinctEvents() {
        val events = mutableListOf<ReviewDeleteEvent>()
        rule.setContent { ReviewDeleteDialog("엔진 항목을 삭제할까요?", "이 항목만 삭제해요.") { events.add(it) } }
        rule.onNodeWithText("취소").performClick()
        rule.onNodeWithText("삭제").performClick()
        rule.runOnIdle { assertEquals(listOf(ReviewDeleteEvent.CANCEL, ReviewDeleteEvent.CONFIRM), events) }
    }
}
