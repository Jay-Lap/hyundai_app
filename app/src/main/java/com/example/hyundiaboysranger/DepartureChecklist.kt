package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

val departureChecks = listOf(
    "안전벨트를 착용했어요", "좌석과 거울을 맞췄어요", "문이 닫혔는지 직접 확인했어요",
    "목적지와 경로를 확인했어요", "필요한 차량 조작을 확인했어요",
)

@Composable
fun DepartureChecklist(checked: Set<Int>, onChange: (Set<Int>) -> Unit, onBack: () -> Unit) {
    Text("출발 전 직접 점검", style = MaterialTheme.typography.headlineMedium)
    Text("${checked.size} / ${departureChecks.size}개 확인 · 직접 체크하는 준비 목록이에요. 차량 센서가 확인한 결과가 아니에요.")
    departureChecks.forEachIndexed { index, label ->
        Row(Modifier.fillMaxWidth().sizeIn(minHeight = 64.dp)
            .toggleable(value = index in checked, role = Role.Checkbox,
                onValueChange = { onChange(if (it) checked + index else checked - index) })
            .padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Checkbox(checked = index in checked, onCheckedChange = null)
            Text(label, modifier = Modifier.weight(1f))
        }
    }
    if (checked.size == departureChecks.size) Text("목록 확인을 마쳤어요. 출발 전 주변 상황과 차량 표시를 직접 확인해 주세요.")
    Text("앱을 다시 시작하거나 주차 확인이 해제되면 목록은 초기화돼요.")
    OutlinedButton(onClick = { onChange(emptySet()) }) { Text("점검 목록 다시 시작") }
    OutlinedButton(onClick = onBack) { Text("홈으로") }
}
