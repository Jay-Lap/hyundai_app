package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/** Presentation only: does not read vehicle state or user storage. */
class ParkingLockedNoticeUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun unknownStateAndUnavailableExplanationAreReachableAtDoubleSize() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.size(280.dp, 220.dp)) {
                    ParkingLockedNotice(ParkingEvidence(), libraryAvailable = false)
                }
            }
        }
        rule.onNodeWithText("기어: 확인 안 됨", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("차량 연결 기능을 사용할 수 없어", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun lastReadingUsesReadableLabelsAndExplainsItMayHaveChanged() {
        rule.setContent {
            ParkingLockedNotice(ParkingEvidence(park = false, speed = 0f, unrestricted = false, observedAt = 100),
                libraryAvailable = true)
        }
        rule.onNodeWithText("마지막 조회값이며 현재 상태와 다를 수 있어요.").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("기어: P단 아님 / 속도: 0.0 m/s / 화면 사용: 제한")
            .performScrollTo().assertIsDisplayed()
    }
}
