package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class WiperStartControlUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun blockedStartExplainsWhyAtDoubleTextSize() {
        val reason = "차량 연동 안전 점검 중이라 연습을 시작할 수 없어요. 조작 설명은 볼 수 있어요."
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Column(Modifier.size(280.dp, 220.dp).verticalScroll(rememberScrollState())) {
                    WiperStartControl(sdkAvailable = false, startAllowed = true, restarting = false) {}
                }
            }
        }
        rule.onNodeWithText(reason).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("연습 시작").performScrollTo().assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, reason))
    }
}
