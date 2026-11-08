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

/** Presentation and in-memory callback only: no SDK or user storage. */
class PreparationConcernUiTest {
    @get:Rule val rule = createComposeRule()
    @Test fun seniorCanReadSourceNoticeAndChooseTopicAtDoubleSize() {
        val events = mutableListOf<ConcernSelected>()
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.size(280.dp, 220.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        ConcernPreparation(ConcernUiState(senior = true)) { events.add(it) }
                    }
                }
            }
        }
        rule.onNodeWithText("실제 경로 분석이나 차량 상태가 아니에요", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("앞유리에 김이 서릴 때").performScrollTo().assertHasClickAction().performClick()
        rule.runOnIdle { assertEquals(listOf(ConcernSelected(PreparationConcern.VISIBILITY)), events) }
    }
}
