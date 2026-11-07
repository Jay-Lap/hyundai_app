package com.example.hyundiaboysranger

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ReviewConsentControlTest {
    @get:Rule val rule = createComposeRule()

    @Test fun disablingRequiresConfirmationAndCancelPreservesConsent() {
        var enabled = true
        rule.setContent {
            var state by remember { mutableStateOf(true) }
            ReviewConsentControl(state) { state = it; enabled = it }
        }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).performClick()
        rule.onNodeWithText("취소").performClick()
        rule.runOnIdle { assertTrue(enabled) }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).performClick()
        rule.onNodeWithText("끄고 기록 삭제").performClick()
        rule.runOnIdle { assertFalse(enabled) }
    }
}
