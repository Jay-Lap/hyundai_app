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

/** Presentation-only tests; no preferences or user storage are opened. */
class SettingsReviewAccessibilityTest {
    @get:Rule val rule = createComposeRule()

    @Test fun resetAtDoubleFontSizeCanBeCancelledAndConfirmed() {
        var resets = 0
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.size(280.dp, 220.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) { SettingsResetControl { resets++ } }
                }
            }
        }
        rule.onNodeWithText("설정 초기화").performScrollTo().assertHasClickAction().performClick()
        rule.onNodeWithText("취소").performClick()
        rule.runOnIdle { assertEquals(0, resets) }
        rule.onNodeWithText("설정 초기화").performClick()
        rule.onNodeWithText("초기화", useUnmergedTree = true).performClick()
        rule.runOnIdle { assertEquals(1, resets) }
    }

    @Test fun reviewActionsAreNamedForTheItemAndDispatchDistinctEvents() {
        val events = mutableListOf<SavedWarningUiEvent>()
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.size(280.dp, 220.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        SavedWarningRow(SavedWarningUiState("엔진 경고등", "기록 시각", false)) { events.add(it) }
                    }
                }
            }
        }
        rule.onNodeWithText("엔진 경고등 학습 다시 열기").performScrollTo().assertHasClickAction().performClick()
        rule.onNodeWithText("엔진 경고등 복습에서 삭제").performScrollTo().assertHasClickAction().performClick()
        rule.runOnIdle { assertEquals(listOf(SavedWarningUiEvent.OPEN, SavedWarningUiEvent.DELETE), events) }
    }
}
