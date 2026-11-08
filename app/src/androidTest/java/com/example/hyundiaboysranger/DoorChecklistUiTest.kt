package com.example.hyundiaboysranger

import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

/** No SDK or user storage: injected fake readers and parking signals only. */
class DoorChecklistUiTest {
    @get:Rule val rule = createComposeRule()
    private class FakeReader : ChecklistDoorReader {
        val response = CompletableDeferred<Map<ChecklistDoor, SignalResult<DoorPosition>>>()
        val closed = AtomicBoolean(false)
        override suspend fun readChecklistDoors() = withContext(NonCancellable) { response.await() }
        override fun close() { closed.set(true) }
    }
    private fun allClosed() = ChecklistDoor.entries.associateWith<ChecklistDoor, SignalResult<DoorPosition>> { SignalResult.Value(DoorPosition.CLOSED) }

    @Test fun sdkAndManualChecksAreDistinctAndUserCanClearBoth() {
        val reader = FakeReader()
        rule.setContent {
            var checked by remember { mutableStateOf(emptySet<Int>()) }
            var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
            LaunchedEffect(Unit) { while (true) { now = SystemClock.elapsedRealtime(); delay(100) } }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DepartureChecklist(checked, { checked = it }, {}, {}, {}, ParkingEvidence(true, 0f, true, now), { reader }, vehicleQueriesAllowed = true)
            }
        }
        rule.onNodeWithText("안전벨트를 착용했어요").performScrollTo().performClick().assertIsOn()
        rule.onNodeWithText("문이 닫혔는지 확인했어요").performScrollTo().performClick()
        rule.onNodeWithText("직접 확인").assertExists()
        rule.onNodeWithText("네 문 상태 조회").performScrollTo().performClick()
        reader.response.complete(allClosed())
        rule.waitUntil(5_000) { reader.closed.get() }
        rule.onNodeWithText("SDK로 확인됨", substring = true).assertExists()
        rule.onNodeWithText("문이 닫혔는지 확인했어요").performScrollTo().performClick()
        rule.onNodeWithText("직접 확인").assertDoesNotExist()
        rule.onNodeWithText("SDK로 확인됨", substring = true).assertDoesNotExist()
        rule.onNodeWithText("문이 닫혔는지 확인했어요").assertIsOff()
        rule.onNodeWithText("안전벨트를 착용했어요").assertIsOn()
        rule.onNodeWithText("점검 목록 다시 시작").performScrollTo().performClick()
        rule.onNodeWithText("안전벨트를 착용했어요").assertIsOff()
    }

    @Test fun resultAfterLeavingCannotCheckReopenedScreen() {
        val reader = FakeReader()
        rule.setContent {
            var visible by remember { mutableStateOf(true) }
            var checked by remember { mutableStateOf(emptySet<Int>()) }
            var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
            LaunchedEffect(Unit) { while (true) { now = SystemClock.elapsedRealtime(); delay(100) } }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (visible) DepartureChecklist(checked, { checked = it }, { visible = false }, {}, {}, ParkingEvidence(true, 0f, true, now), { reader }, vehicleQueriesAllowed = true)
                else Button(onClick = { visible = true }) { Text("다시 열기") }
            }
        }
        rule.onNodeWithText("네 문 상태 조회").performScrollTo().performClick()
        rule.onNodeWithText("홈으로").performScrollTo().performClick()
        reader.response.complete(allClosed())
        rule.waitUntil(5_000) { reader.closed.get() }
        rule.onNodeWithText("다시 열기").performClick()
        rule.onNodeWithText("SDK로 확인됨", substring = true).assertDoesNotExist()
        rule.onNodeWithText("문이 닫혔는지 확인했어요").assertIsOff()
    }

    @Test fun manualUncheckCancelsPendingResult() {
        val reader = FakeReader()
        rule.setContent {
            var checked by remember { mutableStateOf(emptySet<Int>()) }
            var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
            LaunchedEffect(Unit) { while (true) { now = SystemClock.elapsedRealtime(); delay(100) } }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DepartureChecklist(checked, { checked = it }, {}, {}, {}, ParkingEvidence(true, 0f, true, now), { reader }, vehicleQueriesAllowed = true)
            }
        }
        rule.onNodeWithText("문이 닫혔는지 확인했어요").performScrollTo().performClick()
        rule.onNodeWithText("네 문 상태 조회").performScrollTo().performClick()
        rule.onNodeWithText("문이 닫혔는지 확인했어요").performScrollTo().performClick()
        reader.response.complete(allClosed())
        rule.waitUntil(5_000) { reader.closed.get() }
        rule.onNodeWithText("SDK로 확인됨", substring = true).assertDoesNotExist()
        rule.onNodeWithText("문이 닫혔는지 확인했어요").assertIsOff()
    }

    @Test fun blockedChecklistShowsSafetyStatusWithoutCreatingReader() {
        var created = 0
        rule.setContent {
            val now = SystemClock.elapsedRealtime()
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DepartureChecklist(emptySet(), {}, {}, {}, {}, ParkingEvidence(true, 0f, true, now), { created++; FakeReader() })
            }
        }
        rule.onNodeWithText("차량 연동 안전 점검 중").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("네 문 상태 조회").performScrollTo().assertIsNotEnabled()
        rule.runOnIdle { assertEquals(0, created) }
    }
}
