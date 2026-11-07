package com.example.hyundiaboysranger

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun VehicleHealthScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var request by remember { mutableIntStateOf(0) }
    var reading by remember { mutableStateOf<VehicleHealth?>(null) }
    var loading by remember { mutableStateOf(false) }
    var timestamp by remember { mutableStateOf("") }
    var startedAt by remember { mutableLongStateOf(0) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { now = SystemClock.elapsedRealtime(); delay(1000) } }
    LaunchedEffect(request) {
        if (request > 0) {
            loading = true; reading = null; startedAt = SystemClock.elapsedRealtime()
            val reader = VehicleHealthReader(context)
            try {
                reading = reader.read()
                timestamp = DateFormat.getTimeInstance().format(Date())
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { reading = VehicleHealth() }
            finally { loading = false; reader.close() }
        }
    }
    Text("차량 상태 쉽게 보기", style = MaterialTheme.typography.headlineMedium)
    Text("SDK가 제공하는 문 상태·안전벨트 경고·워셔액 경고를 조회해요. 모든 경고등을 감시하는 기능은 아니에요.")
    Button(onClick = { request++ }, enabled = !loading) { Text(if (loading) "조회 중…" else "차량 상태 조회") }
    if (reading == null && !loading) Text("아직 조회하지 않았어요.")
    reading?.let { snapshot ->
        Text("마지막 조회 $timestamp · 항목마다 조회 시점이 조금 다를 수 있어요.")
        if (now - startedAt > 15_000) Text("조회한 지 시간이 지났어요. 현재 상태는 다시 조회해 주세요.")
        if (snapshot.doors.isEmpty()) Text("문 정보를 확인하지 못했어요. 지원 여부와 연결·권한을 확인해 주세요.")
        snapshot.doors.forEach { door ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(door.name, style = MaterialTheme.typography.titleLarge)
                    Text(door.explanation)
                    Text("잠금 신호: ${when (val result = door.lockResult) { is SignalResult.Value -> if (result.value) "잠김" else "잠금 해제"; else -> signalStatus(result) }}")
                }
            }
        }
        WarningReading("앞 왼쪽 좌석 안전벨트 경고", snapshot.beltResult,
            "차량이 안전벨트 미착용 경고를 보내고 있어요. 안전벨트와 차량 표시를 직접 확인해 주세요.")
        WarningReading("워셔액 부족 경고", snapshot.washerResult,
            "차량이 워셔액 부족 경고를 보내고 있어요. 해당 차량의 공식 매뉴얼에서 워셔액 점검 방법을 확인해 주세요.")
    }
    Text("확인 안 됨은 정상이라는 뜻이 아니에요. 차량 표시와 공식 매뉴얼을 우선 확인해 주세요. 이 앱은 문이나 잠금 장치를 제어하지 않아요.")
    OutlinedButton(onClick = onBack) { Text("홈으로") }
}

@Composable
private fun WarningReading(title: String, warning: SignalResult<Boolean>, explanation: String) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(when (warning) {
        is SignalResult.Value -> if (warning.value) explanation else "이번 조회에서 해당 경고는 보고되지 않았어요."
        else -> signalStatus(warning)
    })
}

@Composable
private fun signalStatus(result: SignalResult<*>): String = androidx.compose.ui.res.stringResource(when (result) {
    SignalResult.NoSignal -> R.string.signal_no_data
    SignalResult.PermissionDenied -> R.string.signal_permission
    SignalResult.Timeout -> R.string.signal_timeout
    SignalResult.EmulatorUnavailable -> R.string.signal_emulator_unavailable
    SignalResult.Error -> R.string.signal_error
    is SignalResult.Value -> R.string.signal_no_data
})
