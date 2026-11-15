package com.example.hyundiaboysranger

import android.os.SystemClock
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.hyundiaboysranger.ui.theme.DesignTokens
import kotlinx.coroutines.delay

@Composable
fun ParkedApp() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var libraryAvailable by remember { mutableStateOf(true) }
    var evidence by remember { mutableStateOf(ParkingEvidence()) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    // Independent watchdog: a hanging vehicle read must not leave the screen unlocked.
    LaunchedEffect(Unit) { while (true) { now = SystemClock.elapsedRealtime(); delay(200) } }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val monitor = ParkingMonitor(context)
            libraryAvailable = monitor.available
            try {
                VehicleStateHub(monitor::read).states().collect { evidence = it }
            } finally { evidence = ParkingEvidence(); monitor.close() }
        }
    }
    // The watchdog is only a recomposition trigger, not the time of this decision.
    // A newly received sample may be newer than its last tick; comparing with that
    // old tick falsely marks the sample as future-dated until the next tick.
    val parkingAllowed = remember(evidence, now) {
        FeatureGate.allowed(AppFeature.PRACTICE, evidence, SystemClock.elapsedRealtime())
    }
    if (parkingAllowed) VariantParkedContent(evidence, currentParking = { evidence })
    else ParkingLockedNotice(evidence, libraryAvailable)
}

@Composable
internal fun ParkingLockedNotice(evidence: ParkingEvidence, libraryAvailable: Boolean) {
    val unknown = stringResource(R.string.parking_locked_unknown)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DesignTokens.gapLarge),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.gapMedium)) {
        Text(stringResource(R.string.parking_locked_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.parking_locked_subtitle), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.parking_locked_requirements))
        Text(stringResource(R.string.driver_responsibility))
        if (evidence.observedAt >= 0) Text(stringResource(R.string.parking_locked_last_read))
        val gear = when (evidence.park) {
            true -> stringResource(R.string.parking_locked_gear_park)
            false -> stringResource(R.string.parking_locked_gear_not_park)
            null -> unknown
        }
        val speed = evidence.speed?.let { stringResource(R.string.parking_locked_speed, it.toString()) } ?: unknown
        val ux = when (evidence.unrestricted) {
            true -> stringResource(R.string.parking_locked_ux_allowed)
            false -> stringResource(R.string.parking_locked_ux_restricted)
            null -> unknown
        }
        Text(stringResource(R.string.parking_locked_values, gear, speed, ux))
        Text(stringResource(if (libraryAvailable) R.string.parking_locked_retry else R.string.parking_locked_unavailable))
    }
}
