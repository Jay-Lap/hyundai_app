package com.example.hyundiaboysranger

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSettingsTest {
    @Test
    fun selectingPresetsAppliesTheirDefaultTextSizes() {
        assertEquals(UserSettings(UserProfile.SENIOR, true), UserSettings().withProfile(UserProfile.SENIOR))
        assertEquals(UserSettings(UserProfile.BEGINNER, false), UserSettings(largeTitle = true).withProfile(UserProfile.BEGINNER))
    }

    @Test
    fun switchingToCustomKeepsTheCurrentTextSize() {
        val existing = UserSettings(UserProfile.SENIOR, true)
        assertEquals(UserSettings(UserProfile.CUSTOM, true), existing.withProfile(UserProfile.CUSTOM))
    }

    @Test
    fun unknownProfileDoesNotSelectAnUnrequestedPreset() {
        assertEquals(UserProfile.CUSTOM, UserProfile.fromKey("unknown"))
    }
}
