package com.example.hyundiaboysranger

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private fun sdkSource(): String = if (Build.FINGERPRINT.contains("generic") ||
    Build.HARDWARE.contains("ranchu") || Build.MODEL.contains("sdk", ignoreCase = true))
    "EMU_SDK · 에뮬레이터" else "SDK · 실차 여부 미검증"

@Composable
fun PracticeScreen(detailed: Boolean = true, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ReviewRepository(context) }
    var consent by remember { mutableStateOf(repository.consent) }
    var progress by remember { mutableStateOf(PracticeProgress.READY) }
    var reading by remember { mutableStateOf<Boolean?>(null) }
    val source = remember { sdkSource() }
    Text("차량 조작 배우기 · 와이퍼", style = MaterialTheme.typography.headlineMedium)
    Text("주차 상태에서 앞유리 와이퍼를 켰다가 꺼 보세요. 조작 위치는 차량 매뉴얼을 확인해 주세요.")
    if (detailed) Text("1. 연습 시작을 누르세요.\n2. 먼저 꺼짐을 확인해요.\n3. 와이퍼를 켜고 작동 확인을 기다리세요.\n4. 다시 끄면 완료돼요.")
    Text("데이터: $source · 차량에 제어 명령을 보내지 않아요.")
    Text("복습 저장에 동의하면 연습 결과를 이 기기에 저장해요. 동의를 끄면 기록도 삭제돼요.")
    Switch(checked = consent, onCheckedChange = {
        consent = it; repository.consent = it
        if (it && progress != PracticeProgress.READY) repository.save(progress == PracticeProgress.COMPLETE, source)
    })
    Text(progress.label, style = MaterialTheme.typography.titleLarge)
    Text("와이퍼: ${when (reading) { true -> "작동 중"; false -> "꺼짐"; null -> "확인 안 됨" }}")
    Text("연결 끊김·권한 부족·지원하지 않는 상태에서는 완료 처리하지 않아요. 주차 확인이 끊기거나 화면을 나가면 연습을 중단해요.")
    Button(onClick = {
        reading = null; progress = PracticeProgress.WAIT_OFF
        repository.save(false, source)
    }, enabled = progress == PracticeProgress.READY || progress == PracticeProgress.COMPLETE) {
        Text(if (progress == PracticeProgress.COMPLETE) "다시 연습" else "연습 시작")
    }
    val running = progress != PracticeProgress.READY && progress != PracticeProgress.COMPLETE
    LaunchedEffect(running) {
        if (running) {
            val reader = WiperReader(context)
            try {
                while (true) {
                    reading = reader.read()
                    progress = progress.observe(reading)
                    if (progress == PracticeProgress.COMPLETE) {
                        repository.save(true, source)
                        break
                    }
                    delay(500)
                }
            } finally { reader.close() }
        }
    }
    OutlinedButton(onClick = onBack) { Text(if (running) "연습 중단 · 홈으로" else "홈으로") }
}

@Composable
fun ReviewScreen(onPractice: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ReviewRepository(context) }
    var record by remember { mutableStateOf(repository.load()) }
    var history by remember { mutableStateOf(repository.history()) }
    var consent by remember { mutableStateOf(repository.consent) }
    var confirmDelete by remember { mutableStateOf(false) }
    Text("내 복습", style = MaterialTheme.typography.headlineMedium)
    Text("이 기기를 함께 쓰는 사람에게도 기록이 보여요. 현재는 사용자별 계정을 구분하지 않아요.")
    Text("복습 기록 저장 · ${if (consent) "켜짐" else "꺼짐"}")
    Text("끄면 현재 기록과 최근 이력이 삭제돼요.")
    Switch(checked = consent, onCheckedChange = {
        repository.consent = it; consent = it
        record = repository.load(); history = repository.history()
    })
    val saved = record
    if (saved == null) Text("저장된 기록이 없어요. 연습 화면에서 저장에 동의하면 기록을 남겨요.")
    else {
        Text("앞유리 와이퍼 · ${if (saved.complete) "완료" else "미완료"}", style = MaterialTheme.typography.titleLarge)
        Text("${DateFormat.getDateTimeInstance().format(Date(saved.updatedAt))} · ${saved.source}")
        Button(onClick = onPractice) { Text("처음부터 다시 연습") }
        OutlinedButton(onClick = { confirmDelete = true }) { Text("기록 삭제") }
    }
    if (history.isNotEmpty()) {
        Text("최근 연습 이력 · 최대 20건", style = MaterialTheme.typography.titleLarge)
        Text("시작과 완료를 각각 기록해요. 시작 기록만 남은 연습은 중간에 종료됐을 수 있어요.")
        history.forEach { item ->
            Text("${DateFormat.getDateTimeInstance().format(Date(item.updatedAt))} · ${if (item.complete) "완료" else "시작/진행 중"} · ${item.source}")
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("복습 기록을 삭제할까요?") }, text = { Text("현재 기록과 최근 이력이 모두 삭제돼요. 삭제한 기록은 복원할 수 없어요.") },
        confirmButton = { TextButton(onClick = { repository.delete(); record = null; history = emptyList(); confirmDelete = false }) { Text("삭제") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } })
    OutlinedButton(onClick = onBack) { Text("홈으로") }
}
