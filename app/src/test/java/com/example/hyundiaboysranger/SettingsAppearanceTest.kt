package com.example.hyundiaboysranger

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsAppearanceTest {
    @Test fun changesOnlyTheChosenAppearanceAndCustomProfile() {
        val original = UserSettings(profile = UserProfile.SENIOR, textSizePercent = 175, preferWideRoads = true, preferHighways = true)
        assertEquals(original.copy(profile = UserProfile.CUSTOM, highContrast = true), reduceAppearance(original, AppearanceEvent.Contrast(true)))
        assertEquals(original.copy(profile = UserProfile.CUSTOM, displayMode = DisplayMode.NIGHT), reduceAppearance(original, AppearanceEvent.Mode(DisplayMode.NIGHT)))
        assertEquals(original.copy(profile = UserProfile.CUSTOM, detailedGuidance = false), reduceAppearance(original, AppearanceEvent.Detailed(false)))
    }
}
