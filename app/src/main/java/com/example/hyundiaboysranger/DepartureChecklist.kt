package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.platform.LocalContext
import android.os.SystemClock
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import com.example.hyundiaboysranger.ui.theme.DesignTokens

@Composable
fun DepartureChecklist(checked: Set<Int>, onChange: (Set<Int>) -> Unit, onBack: () -> Unit,
    onReview: () -> Unit, onRoute: () -> Unit, parkingEvidence: ParkingEvidence? = null,
    readerFactory: (Context) -> ChecklistDoorReader = { VehicleHealthReader(it) },
    currentParking: () -> ParkingEvidence? = { parkingEvidence },
    vehicleQueriesAllowed: Boolean = VehicleSdkAccess.enabled) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { DoorChecklistController() }
    var doorState by remember { mutableStateOf(controller.state) }
    var request by remember { mutableStateOf<Job?>(null) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val currentParkingProvider by rememberUpdatedState(currentParking)
    fun evidenceNow(): ParkingEvidence = currentParkingProvider() ?: ParkingEvidence()
    DisposableEffect(controller) {
        onDispose { controller.close(); request?.cancel() }
    }
    LaunchedEffect(parkingEvidence) {
        controller.refresh(evidenceNow(), SystemClock.elapsedRealtime())
        doorState = controller.state
        if (!doorState.loading) request?.cancel()
    }
    LaunchedEffect(controller) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            controller.refresh(evidenceNow(), now)
            doorState = controller.state
            if (!doorState.loading) request?.cancel()
            delay(200)
        }
    }
    val departureChecks = stringArrayResource(R.array.departure_checks)
    val decisionTime = remember(now, parkingEvidence) { SystemClock.elapsedRealtime() }
    val gateAllowed = FeatureGate.allowed(AppFeature.CHECKLIST, evidenceNow(), decisionTime)
    val snapshot = doorState.snapshot
    val fresh = snapshot != null && decisionTime - snapshot.requestedAt in 0..15_000
    val sdkChecked = doorState.confirmed && gateAllowed && fresh
    val visibleChecks = displayedChecklistChecks(checked, sdkChecked)
    Text(stringResource(R.string.checklist_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.checklist_count, visibleChecks.size, departureChecks.size))
    departureChecks.forEachIndexed { index, label ->
        val isChecked = index in visibleChecks
        Row(Modifier.fillMaxWidth().sizeIn(minHeight = DesignTokens.seniorTouchMinimum)
            .toggleable(value = isChecked, role = Role.Checkbox,
                onValueChange = {
                    if (index == DOOR_CHECK_INDEX && !it) {
                        controller.clear(); doorState = controller.state; request?.cancel()
                    }
                    onChange(updateManualCheck(checked, index, it))
                })
            .padding(DesignTokens.gapSmall), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Checkbox(checked = isChecked, onCheckedChange = null)
            Column(Modifier.weight(1f)) {
                Text(label)
                if (index == DOOR_CHECK_INDEX && index in checked) Text(stringResource(R.string.checklist_manual))
                if (index == DOOR_CHECK_INDEX && sdkChecked) Text(stringResource(R.string.checklist_sdk_confirmed))
            }
        }
    }
    Button(onClick = {
        val token = controller.begin(evidenceNow(), SystemClock.elapsedRealtime())
        doorState = controller.state
        if (token != null) {
            request?.cancel()
            request = scope.launch {
                val results = try {
                    withContext(Dispatchers.IO) {
                        val reader = readerFactory(context)
                        try { reader.readChecklistDoors() } finally { reader.close() }
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { ChecklistDoor.entries.associateWith { SignalResult.Error } }
                catch (_: LinkageError) { ChecklistDoor.entries.associateWith { SignalResult.Error } }
                controller.complete(token, evidenceNow(), SystemClock.elapsedRealtime(), System.currentTimeMillis(), results)
                doorState = controller.state
            }
        }
    }, enabled = vehicleQueriesAllowed && gateAllowed && !doorState.loading, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(if (doorState.loading) R.string.checklist_querying else R.string.checklist_query)) }
    if (!vehicleQueriesAllowed) Text(stringResource(R.string.vehicle_safety_blocked))
    if (!gateAllowed) Text(stringResource(R.string.checklist_parking_required))
    snapshot?.let { last ->
        Text("마지막 조회 완료 ${DateFormat.getTimeInstance().format(Date(last.displayedTime))} · 마지막 조회값이며 현재와 다를 수 있어요. 문마다 조회 시각이 다를 수 있어요.")
        ChecklistDoor.entries.forEach { door ->
            val result = last.results[door] ?: SignalResult.NoSignal
            val label = when (result) {
                is SignalResult.Value -> when (result.value) { DoorPosition.CLOSED -> "닫힘"; DoorPosition.OPEN -> "열림"; DoorPosition.UNKNOWN -> "확인 안 됨" }
                else -> signalStatus(result)
            }
            Text("${door.label}: $label")
        }
        if (!fresh) Text("조회값이 오래되어 SDK 확인을 해제했어요. 다시 조회하거나 직접 확인해 주세요.")
        else if (!sdkChecked && allChecklistDoorsClosed(last.results)) Text("SDK 자동 확인이 해제됐어요. 직접 확인하거나 다시 조회해 주세요.")
        else if (!sdkChecked) Text("네 문이 모두 닫혔다고 SDK로 확인하지 못했어요. 직접 확인하거나 다시 조회해 주세요.")
    }
    if (visibleChecks.size == departureChecks.size) Text(stringResource(R.string.checklist_done))
    Text(stringResource(R.string.checklist_reset_notice))
    OutlinedButton(onClick = { controller.clear(clearSnapshot = true); doorState = controller.state; request?.cancel(); onChange(emptySet()) }) { Text(stringResource(R.string.checklist_reset)) }
    OutlinedButton(onClick = onReview) { Text(stringResource(R.string.checklist_review)) }
    OutlinedButton(onClick = onRoute) { Text(stringResource(R.string.checklist_route)) }
    OutlinedButton(onClick = onBack) { Text(stringResource(R.string.checklist_home)) }
}
