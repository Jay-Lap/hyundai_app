package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/** Presentation-only: the commit callback is a fake and never opens user settings. */
class SettingsTextSizeAccessibilityTest {
    @get:Rule val rule = createComposeRule()
    @Test fun sliderHasAccessibleNameAndCanBeAdjustedAtDoubleFontInNarrowViewport() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.size(280.dp, 220.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) { SettingsTextSizeControl(140) {} }
                }
            }
        }
        rule.onNodeWithContentDescription("글자 크기 조절").performScrollTo()
            .assertIsDisplayed().performSemanticsAction(SemanticsActions.SetProgress) { it(175f) }
        rule.onNodeWithText("조절할 크기 · 175%", substring = true).assertExists()
    }
}
