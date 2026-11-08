package com.example.hyundiaboysranger

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import org.junit.Rule
import org.junit.Test

class ChecklistAccessibilityTest {
    @get:Rule val rule = createComposeRule()

    @Test fun firstCheckRemainsLabeledAndToggleableAtDoubleFontScale() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                var checked by remember { mutableStateOf(emptySet<Int>()) }
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    DepartureChecklist(checked, { checked = it }, {}, {}, {})
                }
            }
        }
        val check = rule.onNodeWithText("안전벨트를 착용했어요")
        check.assertHasClickAction().assertIsOff().performClick().assertIsOn()
    }
}
