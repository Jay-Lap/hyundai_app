package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class SettingsBackupTest {
    @Test fun roundTripKeepsAllSettings() {
        val settings = UserSettings(UserProfile.SENIOR, true, true, false, true, false, DisplayMode.NIGHT, 200)
        assertEquals(settings, SettingsBackup.decode(SettingsBackup.encode(settings)))
        assertEquals(UserSettings(), SettingsBackup.decode(SettingsBackup.encode(UserSettings())))
    }
    @Test fun invalidFilesNeverProducePartialSettings() {
        val valid = SettingsBackup.encode(UserSettings())
        listOf("", valid.replace("v1", "v9"), valid + "profile=SENIOR\n", valid.replace("textSizePercent=default", "textSizePercent=300"),
            valid.replace("largeTitle=false", "largeTitle=maybe"), valid.replace("profile=CUSTOM", "profile=other"),
            "x".repeat(4097), valid.replace("displayMode=SYSTEM\n", "")).forEach { assertNull(SettingsBackup.decode(it)) }
    }
}
