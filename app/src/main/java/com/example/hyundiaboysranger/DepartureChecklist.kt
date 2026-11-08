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
internal fun DoorSnapshotSection(last: DoorChecklistSnapshot, fresh: Boolean, sdkChecked: Boolean) {
    Text(stringResource(R.string.checklist_last_read, DateFormat.getTimeInstance().format(Date(last.displayedTime))))
    ChecklistDoor.entries.forEach { door ->
        val result = last.results[door] ?: SignalResult.NoSignal
        val label = when (result) {
            is SignalResult.Value -> stringResource(when (result.value) { DoorPosition.CLOSED -> R.string.checklist_closed; DoorPosition.OPEN -> R.string.checklist_open; DoorPosition.UNKNOWN -> R.string.checklist_unknown })
            else -> signalStatus(result)
        }
        val name = stringResource(when (door) { ChecklistDoor.FRONT_LEFT -> R.string.door_front_left; ChecklistDoor.FRONT_RIGHT -> R.string.door_front_right; ChecklistDoor.REAR_LEFT -> R.string.door_rear_left; ChecklistDoor.REAR_RIGHT -> R.string.door_rear_right })
        Text(stringResource(R.string.checklist_door_result, name, label))
    }
    if (!fresh) Text(stringResource(R.string.checklist_stale))
    else if (!sdkChecked && allChecklistDoorsClosed(last.results)) Text(stringResource(R.string.checklist_sdk_cleared))
    else if (!sdkChecked) Text(stringResource(R.string.checklist_not_verified))
}

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
    snapshot?.let { DoorSnapshotSection(it, fresh, sdkChecked) }
    if (visibleChecks.size == departureChecks.size) Text(stringResource(R.string.checklist_done))
    Text(stringResource(R.string.checklist_reset_notice))
    OutlinedButton(onClick = { controller.clear(clearSnapshot = true); doorState = controller.state; request?.cancel(); onChange(emptySet()) }, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.checklist_reset)) }
    OutlinedButton(onClick = onReview, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.checklist_review)) }
    OutlinedButton(onClick = onRoute, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.checklist_route)) }
    OutlinedButton(onClick = onBack, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.checklist_home)) }
}
