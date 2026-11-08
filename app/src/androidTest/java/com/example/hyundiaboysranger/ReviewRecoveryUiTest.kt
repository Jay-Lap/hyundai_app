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
import android.content.SharedPreferences
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Every case uses a distinct preference file; existing user data is never read or changed. */
class ReviewRecoveryUiTest {
    @get:Rule val rule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun unknownDeleteRetryCanBeCancelledWithoutClaimingRollback() {
        val prefs = context.getSharedPreferences("unknown_ui_${UUID.randomUUID()}", 0)
        prefs.edit().putBoolean("consent", true).putBoolean("complete", true).putLong("updated", 123L).commit()
        val fail = AtomicBoolean(true)
        val attempts = AtomicInteger()
        val injected = object : SharedPreferences by prefs {
            override fun edit(): SharedPreferences.Editor {
                val editor = prefs.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun commit(): Boolean { attempts.incrementAndGet(); return if (fail.get()) false else editor.commit() }
                }
            }
        }
        val repository = ReviewRepository(injected)
        assertFalse(repository.delete())
        rule.setContent {
            var status by remember { mutableStateOf(repository.storageStatus()) }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ReviewRecoverySection(repository, status) { status = repository.storageStatus() }
            }
        }
        rule.onNodeWithText("복습 기록 삭제의 저장 결과를 확인하지 못했어요", substring = true).assertExists()
        rule.onNodeWithText("손상 기록 초기화").assertDoesNotExist()
        rule.onNodeWithText("같은 복습 작업 다시 시도").performClick()
        rule.onNodeWithText("취소해도 이전 시도가 취소되거나 복구되지는 않아요", substring = true).assertExists()
        rule.onNodeWithText("취소").performClick()
        rule.runOnIdle {
            assertEquals(1, attempts.get())
            assertEquals(ReviewStorageStatus.WRITE_UNKNOWN, repository.storageStatus())
            assertTrue(prefs.contains("complete"))
            fail.set(false)
        }
        rule.onNodeWithText("같은 복습 작업 다시 시도").performClick()
        rule.onNodeWithText("다시 시도").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { repository.storageStatus() == ReviewStorageStatus.NORMAL }
        rule.runOnIdle { assertFalse(prefs.contains("complete")); assertEquals(2, attempts.get()) }
    }

    @Test fun previewCancelPreservesAndConfirmedResetClearsOnlyReviewFields() {
        val name = "recovery_ui_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        assertTrue(prefs.edit().putBoolean("consent", true).putLong("updated", 123L).putString("other", "keep").commit())
        val original = prefs.all.toMap()
        val repository = ReviewRepository(context, name)
        rule.setContent {
            var status by remember { mutableStateOf(repository.storageStatus()) }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ReviewRecoverySection(repository, status) { status = repository.storageStatus() }
            }
        }
        rule.onNodeWithText("손상 기록 초기화").assertDoesNotExist()
        rule.onNodeWithText("저장된 내용 확인").performClick()
        rule.onNodeWithText("updated: 123", substring = true).assertExists()
        rule.onNodeWithText("손상 기록 초기화").performClick()
        rule.onNodeWithText("취소").performClick()
        rule.runOnIdle { assertEquals(original, prefs.all) }
        rule.onNodeWithText("손상 기록 초기화").performClick()
        rule.onNodeWithText("초기화 확인").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { repository.storageStatus() == ReviewStorageStatus.NORMAL }
        rule.runOnIdle {
            assertEquals(ReviewStorageStatus.NORMAL, repository.storageStatus())
            assertEquals(true, prefs.all["consent"])
            assertEquals("keep", prefs.all["other"])
            assertFalse(prefs.contains("updated"))
        }
    }

    @Test fun futureVersionIsReadOnlyAndOffersNoReset() {
        val name = "future_ui_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        prefs.edit().putInt("schema_version", 9).putBoolean("consent", true).putString("future", "keep").commit()
        val original = prefs.all.toMap()
        val repository = ReviewRepository(context, name)
        assertEquals(ReviewStorageStatus.FUTURE_VERSION, repository.storageStatus())
        assertFalse(repository.hasDamagedData())
        rule.setContent { ReviewRecoverySection(repository, repository.storageStatus()) {} }
        rule.onNodeWithText("이 기록은 더 새로운 앱 버전에서 저장된 형식", substring = true).assertExists()
        rule.onNodeWithText("손상 기록 초기화").assertDoesNotExist()
        repository.consent = false
        repository.delete()
        repository.save(true, "test")
        assertNull(repository.recoverySnapshot())
        assertEquals(original, prefs.all)
    }

    @Test fun changedSnapshotCannotBeReset() {
        val name = "stale_ui_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        prefs.edit().putBoolean("consent", true).putLong("updated", 10L).commit()
        val repository = ReviewRepository(context, name)
        val snapshot = repository.recoverySnapshot()!!
        prefs.edit().putLong("updated", 20L).commit()
        val changed = prefs.all.toMap()
        assertFalse(repository.resetDamagedAfterConfirmation(snapshot))
        assertEquals(changed, prefs.all)
    }
}
