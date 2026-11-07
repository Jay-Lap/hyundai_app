package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp

@Composable
fun SettingsManagement(onReset: () -> Unit) {
    val transfer = LocalSettingsTransfer.current
    var confirmReset by remember { mutableStateOf(false) }
    LocalSettingsBackupScreen()
    Text("파일로 설정 내보내기·가져오기", style = MaterialTheme.typography.titleLarge)
    Text("파일 선택기를 제공하지 않는 기기에서는 위의 내부 백업을 이용해 주세요.")
    Text(transfer.message)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = transfer.export, enabled = !transfer.busy) { Text("설정 파일 저장") }
        OutlinedButton(onClick = transfer.import, enabled = !transfer.busy) { Text("설정 파일 가져오기") }
    }
    transfer.pending?.let { imported ->
        Text("복원할 설정: ${imported.profile.label} · 글자 ${imported.effectiveTextSizePercent}% · ${imported.displayMode.label}")
        Text("현재 사용자 유형·화면 설정·경로 선호가 파일의 값으로 바뀌어요.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = transfer.confirm) { Text("이 설정으로 복원") }
            OutlinedButton(onClick = transfer.cancel) { Text("복원 취소") }
        }
    }
    OutlinedButton(onClick = { confirmReset = true }) { Text("설정 초기화") }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
        title = { Text("설정을 처음 상태로 되돌릴까요?") },
        text = { Text("사용자 유형·글자·색상·경로 선호가 초기화돼요. 복습 기록은 내 복습에서 별도로 관리해요.") },
        confirmButton = { TextButton(onClick = { onReset(); confirmReset = false }) { Text("초기화") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("취소") } })
}
