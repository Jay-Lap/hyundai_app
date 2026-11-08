package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class WarningReviewUiIsolationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun consentCancelDoesNotWriteAndExplicitConsentAddsOnlyChosenItem() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "warning_ui_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        val repo = ReviewRepository(context, name)
        rule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                WarningCatalogScreen(onBack = {}, focusId = "engine", reviewRepository = repo)
            }
        }
        rule.onNodeWithText("복습에 추가").performScrollTo().performClick()
        rule.onNodeWithText("차량의 경고등 감지 기록이 아니에요", substring = true).assertExists()
        rule.onNodeWithText("취소").performClick()
        rule.runOnIdle { assertTrue(prefs.all.isEmpty()) }
        rule.onNodeWithText("복습에 추가").performScrollTo().performClick()
        rule.onNodeWithText("동의하고 추가").performClick()
        rule.waitUntil(5_000) { repo.items().any { it.id == "engine" } }
        rule.runOnIdle {
            assertEquals(listOf("engine"), repo.items().map { it.id })
            assertTrue(repo.consent)
        }
        rule.onNodeWithText("복습에 추가됨").assertIsNotEnabled()
    }
}
