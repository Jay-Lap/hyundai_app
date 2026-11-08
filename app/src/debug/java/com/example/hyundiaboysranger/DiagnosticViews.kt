package com.example.hyundiaboysranger

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable fun DiagnosticEntry(onOpen: () -> Unit) {
    OutlinedButton(onClick = onOpen) { Text("개발용 진단") }
}
@Composable fun DiagnosticScreen(preferHighways: Boolean, onBack: () -> Unit) {
    Text("개발용 진단", style = MaterialTheme.typography.headlineMedium)
    Text("주차 확인 중에만 사용해요. 조회 당시 값이며 미확인은 정상이라는 뜻이 아니에요.")
    VehicleHealthScreen(onBack = onBack)
    NavigationScreen(preferHighways = preferHighways)
    OutlinedButton(onClick = onBack) { Text("홈으로") }
}
@Composable fun NavigationDebugDetails(session: NavigationSession) {
    Text("내비게이션 진단 (화면을 나가면 삭제)")
    if (session.diagnostic.isEmpty()) Text("아직 요청이나 응답이 없어요.")
    session.diagnostic.forEach { Text(it) }
}
