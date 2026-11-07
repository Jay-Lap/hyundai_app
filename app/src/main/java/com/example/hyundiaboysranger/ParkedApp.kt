package com.example.hyundiaboysranger

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
    if (parkingAllowed) DrivingHelperApp()
    else Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("안전 운전 도우미", style = MaterialTheme.typography.headlineLarge)
        Text("안전한 주차 상태를 확인하면 사용할 수 있어요.", style = MaterialTheme.typography.headlineMedium)
        Text("P단 · 속도 0 · 차량 화면 사용 허용이 모두 필요해요. 주행 중에는 차량의 기본 안내를 이용해 주세요.")
        Text("주차: ${evidence.park ?: "확인 안 됨"} / 속도: ${evidence.speed?.let { "${it} m/s" } ?: "확인 안 됨"} / 화면 허용: ${evidence.unrestricted ?: "확인 안 됨"}")
        if (!libraryAvailable) Text("이 Pleos 이미지에는 android.car 라이브러리가 없어 주차 상태를 확인할 수 없어요. Pleos 전용 상태 연동이 필요해요.")
        else Text("P단에서도 이 화면이 유지되면 앱의 차량 속도 권한과 차량 서비스 연결을 확인해 주세요. 자동으로 다시 확인해요.")
    }
}
