package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

/** UI uses an in-memory overview and never opens user storage. */
class TodayReviewUiTest {
    @get:Rule val rule = createComposeRule()
    @Test fun seniorSeesOnlyTheNewestItem() {
        val items = listOf(ReviewItem("wiper", "front_wiper", "started", 30), ReviewItem("warning", "engine", "saved", 20))
        rule.setContent { Column { TodayReviewSection(ReviewOverview(items, emptyList()), UserProfile.SENIOR) {} } }
        rule.onNodeWithText("와이퍼 연습 · 진행 중 또는 미완료").assertExists()
        rule.onNodeWithText("경고등 학습", substring = true).assertDoesNotExist()
    }

    @Test fun beginnerCanOpenEitherReviewKind() {
        var opened: String? = null
        val items = listOf(ReviewItem("wiper", "front_wiper", "started", 30), ReviewItem("warning", "engine", "saved", 20))
        rule.setContent { Column { TodayReviewSection(ReviewOverview(items, emptyList()), UserProfile.BEGINNER) { opened = it.id } } }
        rule.onNodeWithText("와이퍼 연습 · 진행 중 또는 미완료").performClick()
        rule.runOnIdle { assertEquals("front_wiper", opened) }
        rule.onNodeWithText("경고등 학습", substring = true).performClick()
        rule.runOnIdle { assertEquals("engine", opened) }
    }

    @Test fun seniorReviewCanBeReachedAtDoubleFontScaleInNarrowViewport() {
        var opened: String? = null
        val items = listOf(ReviewItem("wiper", "front_wiper", "started", 30))
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.size(width = 320.dp, height = 240.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        TodayReviewSection(ReviewOverview(items, emptyList()), UserProfile.SENIOR) { opened = it.id }
                    }
                }
            }
        }
        rule.onNodeWithText("와이퍼 연습 · 진행 중 또는 미완료").performScrollTo().assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals("front_wiper", opened) }
    }

    @Test fun laterAndCloseDoNotAcceptTheProposal() {
        var accepted = 0; var dismissed = 0
        rule.setContent { Column { GuidanceProposal(false, onAccept = { accepted++ }, onDismiss = { dismissed++ }) } }
        rule.onNodeWithText("나중에 보기").performClick()
        rule.onNodeWithText("닫기").performClick()
        rule.runOnIdle { assertEquals(0, accepted); assertEquals(2, dismissed) }
        rule.onNodeWithText("상세 안내 켜기").performClick()
        rule.onNodeWithText("취소").performClick()
        rule.runOnIdle { assertEquals(0, accepted) }
        rule.onNodeWithText("상세 안내 켜기").performClick()
        rule.onNodeWithText("켜기").performClick()
        rule.runOnIdle { assertEquals(1, accepted) }
    }
}
