package com.example.hyundiaboysranger

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferenceName = "settings_test_${UUID.randomUUID()}"

    @After
    fun cleanUp() {
        context.deleteSharedPreferences(preferenceName)
    }

    @Test
    fun savesAndRestoresSettingsWithANewRepository() {
        val settings = UserSettings(profile = UserProfile.SENIOR, largeTitle = true)
        SettingsRepository(context, preferenceName).save(settings)

        assertEquals(settings, SettingsRepository(context, preferenceName).load())
    }

    @Test
    fun unknownProfileFallsBackToCustomWithoutLosingTitleSetting() {
        context.getSharedPreferences(preferenceName, 0).edit()
            .putString("profile", "unknown-profile")
            .putBoolean("large_title", true)
            .commit()

        assertEquals(UserSettings(UserProfile.CUSTOM, true), SettingsRepository(context, preferenceName).load())
    }
}
