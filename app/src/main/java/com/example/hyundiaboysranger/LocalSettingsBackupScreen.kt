package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun LocalSettingsBackupScreen() {
    val context = LocalContext.current
    val repository = remember { LocalSettingsBackupRepository(context) }
    val current = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var backup by remember { mutableStateOf(repository.load()) }
    var pendingSave by remember { mutableStateOf<UserSettings?>(null) }
    var pendingRestore by remember { mutableStateOf<LocalSettingsBackup?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    fun save(settings: UserSettings) {
        busy = true
        scope.launch {
            val success = withContext(Dispatchers.IO) { repository.save(settings) }
            backup = repository.load()
            message = if (success) "현재 설정을 앱 안에 백업했어요." else "백업 저장에 실패했어요. 다시 시도해 주세요."
            busy = false
        }
    }
    Text("이 기기에 설정 백업", style = MaterialTheme.typography.titleLarge)
    Text("파일 선택 화면 없이 앱 안에 백업 1개를 보관해요. 앱 삭제·데이터 초기화 시 사라지며 다른 기기로 옮기는 용도는 아니에요.")
    backup?.let { Text("백업 시각: ${DateFormat.getDateTimeInstance().format(Date(it.savedAt))}") }
        ?: Text("아직 저장된 내부 백업이 없어요.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = {
            val settings = current.load()
            if (backup == null) save(settings) else pendingSave = settings
        }, enabled = !busy) { Text(if (busy) "저장 중…" else "현재 설정 내부 백업") }
        OutlinedButton(onClick = { pendingRestore = repository.load() }, enabled = backup != null && !busy) { Text("내부 백업 복원") }
    }
    if (message.isNotEmpty()) Text(message)
    pendingSave?.let { captured ->
        AlertDialog(onDismissRequest = { pendingSave = null }, title = { Text("기존 내부 백업을 교체할까요?") },
            text = { Text("이전에 저장한 백업 1개가 현재 설정으로 바뀌어요. 복습 기록은 변경하지 않아요.") },
            confirmButton = { TextButton(onClick = { pendingSave = null; save(captured) }) { Text("교체 저장") } },
            dismissButton = { TextButton(onClick = { pendingSave = null }) { Text("취소") } })
    }
    pendingRestore?.let { saved ->
        val settings = saved.settings
        AlertDialog(onDismissRequest = { pendingRestore = null }, title = { Text("내부 백업으로 복원할까요?") },
            text = { Text("${settings.profile.label} · 글자 ${settings.effectiveTextSizePercent}% · ${settings.displayMode.label}\n" +
                "높은 대비 ${if (settings.highContrast) "켜짐" else "꺼짐"} · 안내 ${if (settings.detailedGuidance) "상세" else "간단"}\n" +
                "화면 설정과 경로 선호를 백업 값으로 되돌려요. 복습 기록은 유지해요.") },
            confirmButton = { TextButton(onClick = {
                current.save(settings); pendingRestore = null; message = "내부 백업의 설정을 적용했어요."
            }) { Text("복원 적용") } },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("취소") } })
    }
}
