package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Unique preference names only; user settings, backup and review stores are not used. */
class SettingsSchemaIsolationTest {
    @get:Rule val rule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun name() = "settings_schema_${UUID.randomUUID()}"

    @Test fun legacyMigrationPreservesAllFieldsAndRepeatsSafely() {
        val name = name(); val prefs = context.getSharedPreferences(name, 0)
        prefs.edit().putString("profile", "senior").putBoolean("large_title", true)
            .putInt("text_size_percent", 175).putString("display_mode", "NIGHT")
            .putBoolean("high_contrast", false).putBoolean("detailed_guidance", true)
            .putBoolean("prefer_wide_roads", true).putBoolean("prefer_highways", false)
            .putString("unknown_key", "preserve").commit()
        val original = prefs.all.toMap(); val repo = SettingsRepository(context, name); val expected = repo.load()
        assertEquals(SettingsStorageKind.LEGACY, repo.status().kind)
        assertTrue(repo.migrateLegacy()); assertTrue(repo.migrateLegacy())
        assertEquals(SettingsStorageKind.CURRENT, repo.status().kind)
        assertEquals(expected, repo.load()); assertEquals(original, prefs.all.filterKeys { it != "schema_version" })
        assertEquals(175, repo.load().textSizePercent); assertTrue(repo.load().preferWideRoads)
    }

    @Test fun corruptAndFutureInputsCannotBeSavedOrMigrated() {
        val inputs = listOf<(android.content.SharedPreferences.Editor) -> Unit>(
            { it.putString("large_title", "bad") },
            { it.putInt("text_size_percent", 250) },
            { it.putInt("schema_version", 1) },
            { it.putInt("schema_version", 99).putString("profile", "future") },
        )
        val expected = listOf(SettingsStorageKind.WRONG_TYPE, SettingsStorageKind.OUT_OF_RANGE, SettingsStorageKind.MISSING, SettingsStorageKind.FUTURE_VERSION)
        inputs.forEachIndexed { index, setup ->
            val name = name(); val prefs = context.getSharedPreferences(name, 0); val edit = prefs.edit(); setup(edit); edit.commit()
            val before = prefs.all.toMap(); val repo = SettingsRepository(context, name)
            assertEquals(expected[index], repo.status().kind)
            assertFalse(repo.save(UserSettings())); assertFalse(repo.migrateLegacy())
            if (expected[index] == SettingsStorageKind.FUTURE_VERSION) assertNull(repo.recoverySnapshot())
            assertEquals(before, prefs.all)
        }
    }

    @Test fun legacyMissingOptionalFieldsKeepsItsDerivedDefaults() {
        val name = name(); val prefs = context.getSharedPreferences(name, 0)
        prefs.edit().putString("profile", "senior").putBoolean("large_title", true).commit()
        val repo = SettingsRepository(context, name); val before = repo.load()
        assertTrue(before.highContrast); assertFalse(before.detailedGuidance); assertNull(before.textSizePercent)
        assertTrue(repo.migrateLegacy()); assertEquals(before, repo.load())
    }

    @Test fun stalePreviewCannotResetChangedSettings() {
        val name = name(); val prefs = context.getSharedPreferences(name, 0)
        prefs.edit().putString("large_title", "broken").commit()
        val repo = SettingsRepository(context, name); val snapshot = repo.recoverySnapshot()!!
        prefs.edit().putString("large_title", "changed").commit()
        val changed = prefs.all.toMap()
        assertFalse(repo.resetDamagedAfterConfirmation(snapshot)); assertEquals(changed, prefs.all)
    }

    @Test fun explicitRecoveryCancelAndConfirmKeepBackupAndReview() {
        val settingsName = name(); val backupName = name(); val reviewName = name()
        val prefs = context.getSharedPreferences(settingsName, 0)
        prefs.edit().putString("profile", "senior").putString("large_title", "broken").putString("unknown", "keep").commit()
        val backup = LocalSettingsBackupRepository(context, backupName)
        assertTrue(backup.save(UserSettings(textSizePercent = 170)))
        val backupBefore = context.getSharedPreferences(backupName, 0).all.toMap()
        val review = context.getSharedPreferences(reviewName, 0)
        review.edit().putBoolean("consent", true).putString("history", "saved").commit()
        val reviewBefore = review.all.toMap(); val before = prefs.all.toMap()
        val repo = SettingsRepository(context, settingsName)
        rule.setContent {
            var status by remember { mutableStateOf(repo.status()) }
            Column(Modifier.verticalScroll(rememberScrollState())) { SettingsRecoverySection(repo, status) { status = repo.status() } }
        }
        rule.onNodeWithText("손상 설정 초기화").assertDoesNotExist()
        rule.onNodeWithText("저장된 설정 내용 확인").performClick()
        rule.onNodeWithText("large_title: broken", substring = true).assertExists()
        rule.onNodeWithText("손상 설정 초기화").performClick()
        rule.onNodeWithText("취소").performClick()
        rule.runOnIdle { assertEquals(before, prefs.all); assertEquals(backupBefore, context.getSharedPreferences(backupName, 0).all); assertEquals(reviewBefore, review.all) }
        rule.onNodeWithText("손상 설정 초기화").performClick()
        rule.onNodeWithText("설정 초기화 확인").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { repo.status().kind == SettingsStorageKind.CURRENT }
        assertEquals(UserSettings(), repo.load()); assertEquals("keep", prefs.all["unknown"])
        assertEquals(backupBefore, context.getSharedPreferences(backupName, 0).all); assertEquals(reviewBefore, review.all)
    }

    @Test fun internalAndFileBackupRestoreRemainCompatible() {
        val settingsName = name(); val backupName = name(); val repo = SettingsRepository(context, settingsName)
        val original = UserSettings(UserProfile.BEGINNER, true, true, true, true, false, DisplayMode.NIGHT, 190)
        assertTrue(repo.save(original))
        val backup = LocalSettingsBackupRepository(context, backupName)
        assertTrue(backup.save(repo.load()))
        val file = SettingsBackup.encode(original)
        assertEquals(original, SettingsBackup.decode(file))
        assertTrue(repo.save(UserSettings()))
        assertTrue(repo.save(backup.load()!!.settings)); assertEquals(original, repo.load())
        assertTrue(repo.save(UserSettings()))
        assertTrue(repo.save(SettingsBackup.decode(file)!!)); assertEquals(original, repo.load())
    }

    @Test fun futureVersionIsExplainedAndOffersNoReset() {
        val name = name(); val prefs = context.getSharedPreferences(name, 0)
        prefs.edit().putInt("schema_version", 8).putString("profile", "new format").commit()
        val original = prefs.all.toMap(); val repo = SettingsRepository(context, name)
        rule.setContent { SettingsRecoverySection(repo, repo.status()) {} }
        rule.onNodeWithText("더 새로운 앱에서 저장한 설정", substring = true).assertExists()
        rule.onNodeWithText("손상 설정 초기화").assertDoesNotExist()
        assertFalse(repo.save(UserSettings())); assertNull(repo.recoverySnapshot()); assertEquals(original, prefs.all)
    }
}
