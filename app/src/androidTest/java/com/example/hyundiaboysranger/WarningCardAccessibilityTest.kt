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

class WarningCardAccessibilityTest {
    @get:Rule val rule = createComposeRule()

    @Test fun narrowDoubleFontCardHasSpecificAccessibleActionAndCanScroll() {
        var selected = 0
        val entry = WarningEntry("engine", "엔진 경고등", "차량의 경고를 확인하세요.", "공식 매뉴얼을 확인하세요.")
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.size(width = 280.dp, height = 220.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        WarningCard(WarningCardState(entry, saved = false, busy = false, storageNormal = true)) { selected++ }
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("엔진 경고등 복습에 추가").performScrollTo().assertIsDisplayed().assertHasClickAction().performClick()
        rule.runOnIdle { assertEquals(1, selected) }
    }
}
