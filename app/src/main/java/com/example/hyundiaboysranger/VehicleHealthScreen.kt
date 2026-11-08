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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.example.hyundiaboysranger.ui.theme.DesignTokens
import java.text.DateFormat
import java.util.Date

@Composable
fun VehicleHealthScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var request by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf(VehicleHealthUiState()) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { now = SystemClock.elapsedRealtime(); delay(1000) } }
    LaunchedEffect(request) {
        if (request > 0) {
            state = reduceVehicleHealth(state, VehicleHealthUiEvent.Started(SystemClock.elapsedRealtime()))
            val reading = try {
                withContext(Dispatchers.IO) {
                    val reader = VehicleHealthReader(context)
                    try { reader.read() } finally { reader.close() }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { val result = signalFailure(error); VehicleHealth(beltResult = result, washerResult = result) }
            catch (_: LinkageError) { VehicleHealth(beltResult = SignalResult.Error, washerResult = SignalResult.Error) }
            state = reduceVehicleHealth(state, VehicleHealthUiEvent.Finished(reading, DateFormat.getTimeInstance().format(Date())))
        }
    }
    Text(stringResource(R.string.health_title), style = MaterialTheme.typography.headlineMedium)
    if (!VehicleSdkAccess.enabled) Text(stringResource(R.string.vehicle_safety_blocked))
    Text(stringResource(R.string.health_description))
    Button(onClick = { request++ }, enabled = VehicleSdkAccess.enabled && !state.loading, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(if (state.loading) R.string.health_loading else R.string.health_query)) }
    if (state.reading == null && !state.loading) Text(stringResource(R.string.health_not_queried))
    state.reading?.let { snapshot ->
        Text(stringResource(R.string.health_last_read, state.timestamp))
        if (now - state.startedAt > 15_000) Text(stringResource(R.string.health_stale))
        if (snapshot.doors.isEmpty()) Text("문 정보를 확인하지 못했어요. 지원 여부와 연결·권한을 확인해 주세요.")
        snapshot.doors.forEach { door ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(DesignTokens.gapMedium), verticalArrangement = Arrangement.spacedBy(DesignTokens.gapSmall)) {
                    Text(door.name, style = MaterialTheme.typography.titleLarge)
                    Text(door.explanation)
                    if (door.positionResult !is SignalResult.Value) Text(signalStatus(door.positionResult))
                    Text("잠금 신호: ${when (val result = door.lockResult) { is SignalResult.Value -> if (result.value) "잠김" else "잠금 해제"; else -> signalStatus(result) }}")
                }
            }
        }
        WarningReading("앞 왼쪽 좌석 안전벨트 경고", snapshot.beltResult,
            "차량이 안전벨트 미착용 경고를 보내고 있어요. 안전벨트와 차량 표시를 직접 확인해 주세요.")
        WarningReading("워셔액 부족 경고", snapshot.washerResult,
            "차량이 워셔액 부족 경고를 보내고 있어요. 해당 차량의 공식 매뉴얼에서 워셔액 점검 방법을 확인해 주세요.")
    }
    Text(stringResource(R.string.health_safety))
    OutlinedButton(onClick = onBack, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.checklist_home)) }
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
internal fun signalStatus(result: SignalResult<*>): String = androidx.compose.ui.res.stringResource(when (result) {
    SignalResult.NoSignal -> R.string.signal_no_data
    SignalResult.ReadNotAvailable -> R.string.signal_read_unavailable
    SignalResult.AreaNotReported -> R.string.signal_area_not_reported
    SignalResult.PermissionDenied -> R.string.signal_permission
    SignalResult.Unsupported -> R.string.signal_unsupported
    SignalResult.Restricted -> R.string.signal_restricted
    SignalResult.ServiceUnavailable -> R.string.signal_service_unavailable
    SignalResult.SafetyBlocked -> R.string.vehicle_safety_blocked
    SignalResult.Timeout -> R.string.signal_timeout
    SignalResult.EmulatorUnavailable -> R.string.signal_emulator_unavailable
    SignalResult.Error -> R.string.signal_error
    is SignalResult.Value -> R.string.signal_no_data
})
