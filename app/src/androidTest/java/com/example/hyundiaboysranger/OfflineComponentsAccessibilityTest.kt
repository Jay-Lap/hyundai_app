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

/** Presentation-only controls; no SDK connection or user data. */
class OfflineComponentsAccessibilityTest {
    @get:Rule val rule = createComposeRule()

    @Test fun connectionControlsRemainReachableInNarrowDoubleFontViewport() {
        val events = mutableListOf<NavigationConnectionEvent>()
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.size(280.dp, 220.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        NavigationConnectionControls(NavigationConnectionState(true, false)) { events.add(it) }
                    }
                }
            }
        }
        rule.onNodeWithText("다시 연결").performScrollTo().assertHasClickAction().performClick()
        rule.onNodeWithText("정보 조회").performScrollTo().assertHasClickAction().performClick()
        rule.runOnIdle { assertEquals(listOf(NavigationConnectionEvent.CONNECT, NavigationConnectionEvent.REFRESH), events) }
    }

    @Test fun warningConsentRequiresExplicitEvent() {
        val events = mutableListOf<WarningConsentEvent>()
        rule.setContent { WarningConsentDialog("엔진 경고등") { events.add(it) } }
        rule.onNodeWithText("차량의 경고등 감지 기록이 아니에요", substring = true).assertExists()
        rule.onNodeWithText("취소").performClick()
        rule.onNodeWithText("동의하고 추가").performClick()
        rule.runOnIdle { assertEquals(listOf(WarningConsentEvent.CANCEL, WarningConsentEvent.ACCEPT), events) }
    }
}
