package com.example.hyundiaboysranger

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSettingsTest {
    @Test
    fun selectingPresetsAppliesTheirDefaultTextSizes() {
        assertEquals(UserSettings(UserProfile.SENIOR, true, highContrast = true, detailedGuidance = false), UserSettings().withProfile(UserProfile.SENIOR))
        assertEquals(UserSettings(UserProfile.BEGINNER, false), UserSettings(largeTitle = true).withProfile(UserProfile.BEGINNER))
    }

    @Test
    fun switchingToCustomKeepsTheCurrentTextSize() {
        val existing = UserSettings(UserProfile.SENIOR, true)
        assertEquals(UserSettings(UserProfile.CUSTOM, true), existing.withProfile(UserProfile.CUSTOM))
    }

    @Test
    fun presetsPreserveRouteAndDayNightPreferences() {
        val original = UserSettings(preferWideRoads = true, preferHighways = true, displayMode = DisplayMode.NIGHT)
        val senior = original.withProfile(UserProfile.SENIOR)
        assertEquals(true, senior.highContrast)
        assertEquals(false, senior.detailedGuidance)
        assertEquals(true, senior.preferWideRoads)
        assertEquals(true, senior.preferHighways)
        assertEquals(DisplayMode.NIGHT, senior.displayMode)
        assertEquals(senior.copy(profile = UserProfile.CUSTOM), senior.withProfile(UserProfile.CUSTOM))
        assertEquals(DisplayMode.SYSTEM, DisplayMode.fromKey("unsupported"))
    }

    @Test
    fun unknownProfileDoesNotSelectAnUnrequestedPreset() {
        assertEquals(UserProfile.CUSTOM, UserProfile.fromKey("unknown"))
    }
    @Test
    fun explicitTextSizeSurvivesProfileSwitchingAndResetRestoresPreset() {
        val manual = UserSettings().withTextSize(160)
        val senior = manual.withProfile(UserProfile.SENIOR)
        val beginner = senior.withProfile(UserProfile.BEGINNER)
        assertEquals(160, senior.effectiveTextSizePercent)
        assertEquals(160, beginner.effectiveTextSizePercent)
        assertEquals(UserProfile.BEGINNER, beginner.withTextSize(180).profile)
        assertEquals(140, senior.copy(textSizePercent = null).effectiveTextSizePercent)
        assertEquals(100, beginner.copy(textSizePercent = null).effectiveTextSizePercent)
        assertEquals(100, manual.withTextSize(0).effectiveTextSizePercent)
        assertEquals(180, manual.withTextSize(999).effectiveTextSizePercent)
    }
}
