package com.example.hyundiaboysranger

import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import kotlin.math.roundToInt
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.activity.compose.BackHandler
import com.example.hyundiaboysranger.ui.theme.accessibleColors
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.example.hyundiaboysranger.ui.theme.HyundiaBoysRangerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
        val runtimePermissions = listOf(
            "android.car.permission.CAR_SPEED", "android.car.permission.CAR_POWERTRAIN",
            "pleos.car.permission.CAR_DOORS", "pleos.car.permission.CAR_WINDOWS",
            "pleos.car.permission.CAR_SAFETY", "pleos.car.permission.NAVI_ROUTE",
            "pleos.car.permission.NAVI_ROUTE_SEARCH",
        ).filter { permission ->
            try {
                val info = packageManager.getPermissionInfo(permission, 0)
                info.protection ==
                    android.content.pm.PermissionInfo.PROTECTION_DANGEROUS &&
                    checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
            } catch (_: PackageManager.NameNotFoundException) { false }
        }
        if (runtimePermissions.isNotEmpty()) permissionRequest.launch(runtimePermissions.toTypedArray())
        setContent {
            HyundiaBoysRangerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (BuildConfig.IS_DEMO) {
                        Column(Modifier.fillMaxSize()) {
                            Text(androidx.compose.ui.res.stringResource(R.string.demo_banner), style = MaterialTheme.typography.titleLarge)
                            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                                VoiceTextSizeHost { SettingsTransferHost { ParkedApp() } }
                            }
                        }
                    } else VoiceTextSizeHost { SettingsTransferHost { ParkedApp() } }
                }
            }
        }
    }
}

@Composable
fun DrivingHelperApp(phonePreview: Boolean = false, parkingEvidence: ParkingEvidence? = null,
    currentParking: () -> ParkingEvidence? = { parkingEvidence }) {
    var screen by rememberSaveable { mutableStateOf("home") }
    var warningFocus by rememberSaveable { mutableStateOf<String?>(null) }
    var warningBackToReview by rememberSaveable { mutableStateOf(false) }
    var checks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val context = LocalContext.current
    val settingsRepository = remember(context) { SettingsRepository(context) }
    val reviewRepository = remember(context) { ReviewRepository(context) }
    val settingsScope = rememberCoroutineScope()
    var settingsBusy by remember { mutableStateOf(false) }
    var settings by remember(settingsRepository) {
        mutableStateOf(if (settingsRepository.status().kind == SettingsStorageKind.WRITE_UNKNOWN) UserSettings() else settingsRepository.load())
    }
    var settingsStatus by remember(settingsRepository) { mutableStateOf(settingsRepository.status()) }
    var overview by remember(screen, reviewRepository) { mutableStateOf<ReviewOverview?>(null) }
    var hiddenProposalEvidence by rememberSaveable { mutableStateOf<String?>(null) }
    var proposalMessage by remember { mutableStateOf("") }

    LaunchedEffect(screen, reviewRepository) {
        if (screen == "home") overview = withContext(Dispatchers.IO) { reviewRepository.overview() }
    }

    DisposableEffect(settingsRepository) {
        val stopObserving = settingsRepository.observe {
            settingsStatus = settingsRepository.status()
            if (settingsStatus.writable) settings = settingsRepository.load()
        }
        onDispose { stopObserving() }
    }
    LaunchedEffect(settingsRepository) {
        if (settingsRepository.status().kind == SettingsStorageKind.LEGACY) {
            val migrated = withContext(Dispatchers.IO) { settingsRepository.migrateLegacy() }
            if (migrated) settings = settingsRepository.load()
            settingsStatus = settingsRepository.status()
        }
    }

    fun updateSettings(updated: UserSettings, description: String = "설정 저장") {
        if (settingsBusy) return
        settingsBusy = true
        settingsScope.launch {
            val saved = withContext(Dispatchers.IO) { settingsRepository.save(updated, description) }
            if (saved) settings = updated
            settingsStatus = settingsRepository.status()
            settingsBusy = false
        }
    }

    val baseTypography = MaterialTheme.typography
    val appTypography = remember(baseTypography, settings.effectiveTextSizePercent) {
        baseTypography.enlarged(settings.effectiveTextSizePercent / 100f)
    }
    val dark = when (settings.displayMode) {
        DisplayMode.SYSTEM -> isSystemInDarkTheme()
        DisplayMode.DAY -> false
        DisplayMode.NIGHT -> true
    }
    val colors = if (settings.highContrast) accessibleColors(dark)
        else if (dark) darkColorScheme() else lightColorScheme()
    BackHandler(enabled = screen != "home") {
        if (screen == "warning_catalog" && warningBackToReview) {
            screen = "review"; warningFocus = null; warningBackToReview = false
        } else { screen = "home"; warningFocus = null; warningBackToReview = false }
    }
    MaterialTheme(typography = appTypography, colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "안전 운전 도우미",
                style = MaterialTheme.typography.headlineLarge
            )
            Text("주차 중 편하게 준비하고 연습해요", style = MaterialTheme.typography.bodyLarge)
            Text(androidx.compose.ui.res.stringResource(R.string.driver_responsibility))
            if (!settingsStatus.writable && screen != "settings") {
                Text(androidx.compose.ui.res.stringResource(R.string.settings_protected_banner))
                OutlinedButton(onClick = { screen = "settings" }) { Text(androidx.compose.ui.res.stringResource(R.string.settings_action_view)) }
            }
            if (phonePreview) InfoCard("휴대폰 에뮬레이터 화면 확인용", "차량 상태와 실제 주차 여부를 확인하지 않아요. 차량 기능은 Pleos 에뮬레이터에서 사용하세요.")

            if (phonePreview && screen in setOf("door", "route", "learn", "warnings")) {
                ScreenTitle("차량 기능은 Pleos에서 사용해요", "이 화면 확인용 모드에는 차량 데이터와 내비게이션이 연결되지 않아요.")
                BackButton { screen = "home" }
            } else when (screen) {
                "home" -> {
                    InfoCard("현재 사용자", if (settingsStatus.writable) "${settings.profile.label} · 사용자 유형과 글자 크기를 기기에 저장해요."
                        else "저장된 설정을 확인하기 전까지 임시 기본값이 적용될 수 있어요.")
                    if (!phonePreview) DiagnosticEntry { screen = "diagnostics" }
                    TodayReviewSection(overview, settings.profile, onOpen = { item ->
                        if (item.kind == "wiper") screen = "learn"
                        else { warningFocus = item.id; warningBackToReview = false; screen = "warning_catalog" }
                    })
                    if (proposalMessage.isNotEmpty()) Text(proposalMessage)
                    val proposalEvidence = overview?.takeIf { hasRepeatedExplicitStops(it.events) }
                        ?.events?.take(4)?.joinToString("|") { "${attemptMarker(it)?.id}:${it.updatedAt}:${attemptMarker(it)?.phase}" }
                    if (proposalEvidence != null && proposalEvidence != hiddenProposalEvidence && !settings.detailedGuidance && settingsStatus.writable) {
                        GuidanceProposal(settingsBusy, onAccept = {
                            if (!settingsBusy) {
                                settingsBusy = true; proposalMessage = "설정을 저장하고 있어요."
                                settingsScope.launch {
                                    val saved = withContext(Dispatchers.IO) { settingsRepository.enableDetailedGuidance() }
                                    settingsStatus = settingsRepository.status()
                                    if (saved) settings = settingsRepository.load()
                                    proposalMessage = if (saved) "상세 안내를 켰어요." else "저장 결과를 확인할 수 없어요. 설정 화면에서 상태를 확인해 주세요."
                                    settingsBusy = false
                                }
                            }
                        }, onDismiss = { hiddenProposalEvidence = proposalEvidence })
                    }
                    if (!settings.detailedGuidance) {
                        NavButton("경로 준비") { screen = "route" }
                        NavButton("차량 조작 배우기") { screen = "learn" }
                        NavButton("내 복습") { screen = "review" }
                        NavButton("출발 전 점검") { screen = "checklist" }
                        NavButton("더 보기") { screen = "more" }
                    } else {
                        InfoCard("출발 전 확인", "경로를 살펴보고, 익숙하지 않은 조작은 주차 중 연습해 보세요.")
                        HomeActions { screen = it }
                    }
                }
                "more" -> {
                    ScreenTitle("더 보기", "문 상태, 도움말과 개인 설정을 확인해요.")
                    HomeActions { screen = it }
                    BackButton { screen = "home" }
                }
                "profile" -> {
                    ScreenTitle("사용자 유형", "고령자: 큰 글자·높은 대비·간단한 안내. 초보자: 보통 글자·상세 안내. 직접 설정은 현재 설정을 유지해요. 따로 지정한 글자 크기는 유형을 바꿔도 유지해요.")
                    UserProfile.entries.forEach { option ->
                        OutlinedButton(onClick = { updateSettings(settings.withProfile(option)) }, enabled = settingsStatus.writable, modifier = Modifier.sizeIn(minHeight = 56.dp)) {
                            Text(if (settings.profile == option) "${option.label} · 선택됨" else option.label)
                        }
                    }
                    NavButton("글자 크기 따로 조절") { screen = "settings" }
                    BackButton { screen = "home" }
                }
                "checklist" -> DepartureChecklist(checks, { checks = it }, { screen = "home" },
                    onReview = { screen = "review" }, onRoute = { screen = "route" }, parkingEvidence = parkingEvidence,
                    currentParking = currentParking)
                "door" -> DoorStatusScreen(onBack = { screen = "home" })
                "route" -> {
                    ScreenTitle("경로 준비", "선호를 저장하고 기본 내비게이션에서 목적지를 검색해요.")
                    Text("큰길 선호 · ${if (settings.preferWideRoads) "켜짐" else "꺼짐"}")
                    androidx.compose.material3.Switch(checked = settings.preferWideRoads, enabled = settingsStatus.writable, onCheckedChange = {
                        updateSettings(settings.copy(preferWideRoads = it))
                    })
                    Text("고속도로 선호 · ${if (settings.preferHighways) "켜짐" else "꺼짐"}")
                    androidx.compose.material3.Switch(checked = settings.preferHighways, enabled = settingsStatus.writable, onCheckedChange = {
                        updateSettings(settings.copy(preferHighways = it))
                    })
                    InfoCard("경로 선호", "큰길 선호는 저장만 해요. 고속도로 선호는 아래 경로 요청 옵션의 초기값에 반영돼요. 적용 버튼을 눌러야 요청돼요.")
                    RouteComparisonScreen(RoutePreferences(settings.preferWideRoads, settings.preferHighways), settings.detailedGuidance)
                    var launchMessage by remember { mutableStateOf("") }
                    Button(onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage("ai.umos.maps.android.navigation.app")
                        launchMessage = try {
                            if (intent == null) "기본 내비게이션 실행 화면을 찾지 못했어요. 차량 홈에서 열어 주세요."
                            else { context.startActivity(intent); "" }
                        } catch (_: Exception) { "내비게이션을 열 수 없어요. 차량 홈에서 실행해 주세요." }
                    }) { Text("기본 내비게이션 열기") }
                    if (launchMessage.isNotEmpty()) Text(launchMessage)
                    NavigationScreen(preferHighways = settings.preferHighways)
                    BackButton { screen = "home" }
                }
                "learn" -> PracticeScreen(detailed = settings.detailedGuidance, onBack = { screen = "home" })
                "warnings" -> VehicleHealthScreen(onBack = { screen = "home" })
                "warning_catalog" -> WarningCatalogScreen(onBack = {
                    screen = if (warningBackToReview) "review" else "home"
                    warningFocus = null; warningBackToReview = false
                }, focusId = warningFocus)
                "diagnostics" -> DiagnosticScreen(settings.preferHighways) { screen = "home" }
                "review" -> ReviewScreen(onPractice = { screen = "learn" }, onWarning = { id ->
                    warningFocus = id; warningBackToReview = true; screen = "warning_catalog"
                }, onBack = { screen = "home" })
                "settings" -> {
                    ScreenTitle("설정", "글자·색상·안내 방식을 바꾸면 즉시 적용돼요.")
                    if (!settingsStatus.writable) {
                        SettingsRecoverySection(settingsRepository, settingsStatus) {
                            settingsStatus = settingsRepository.status()
                            if (settingsStatus.writable) settings = settingsRepository.load()
                        }
                    } else {
                    Text("글자 크기 · ${settings.effectiveTextSizePercent}%")
                    Text("직접 고른 크기는 고령자·초보자를 바꿔도 유지돼요.")
                    var sliderValue by remember(settings.effectiveTextSizePercent) {
                        mutableFloatStateOf(settings.effectiveTextSizePercent.toFloat())
                    }
                    Text("조절할 크기 · ${sliderValue.roundToInt()}% (100~200%)")
                    Slider(value = sliderValue, onValueChange = { sliderValue = it },
                        onValueChangeFinished = { updateSettings(settings.withTextSize(sliderValue.roundToInt())) },
                        valueRange = 100f..200f,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "글자 크기 조절" })
                    Text("손잡이를 끌고 놓으면 적용돼요.")
                    val voice = LocalVoiceTextSize.current
                    if (voice.available) OutlinedButton(onClick = voice.launch) { Text("음성으로 크기 바꾸기") }
                    Text(voice.message)
                    OutlinedButton(onClick = { updateSettings(settings.copy(textSizePercent = null)) }) {
                        Text("사용자 유형 기본 크기로")
                    }
                    Text("높은 대비 · ${if (settings.highContrast) "켜짐" else "꺼짐"}")
                    Switch(modifier = Modifier.semantics { contentDescription = "높은 대비" }, checked = settings.highContrast, onCheckedChange = {
                        updateSettings(settings.copy(profile = UserProfile.CUSTOM, highContrast = it))
                    })
                    Text("화면 모드 · ${settings.displayMode.label}")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DisplayMode.entries.forEach { mode ->
                            OutlinedButton(onClick = { updateSettings(settings.copy(profile = UserProfile.CUSTOM, displayMode = mode)) }) {
                                Text(mode.label + if (settings.displayMode == mode) " · 선택됨" else "")
                            }
                        }
                    }
                    Text("안내 상세도 · ${if (settings.detailedGuidance) "상세" else "간단"}")
                    Switch(modifier = Modifier.semantics { contentDescription = "상세 안내" }, checked = settings.detailedGuidance, onCheckedChange = {
                        updateSettings(settings.copy(profile = UserProfile.CUSTOM, detailedGuidance = it))
                    })
                    Text("간단한 안내는 홈 메뉴와 연습 설명을 줄여요. 안전 안내와 연결 상태는 계속 표시해요.")
                    InfoCard("화면 예시", "설정은 지금 보는 제목·본문·버튼에도 적용돼요.")
                    Text("글자 크기는 사용자 유형과 별도로 저장돼요. 대비·화면 모드·상세도를 바꾸면 직접 설정으로 바뀌어요.")
                    SettingsManagement { updateSettings(UserSettings(), "설정 초기화") }
                    }
                    BackButton { screen = "home" }
                }
            }
        }
    }
}

}

@Composable
internal fun GuidanceProposal(busy: Boolean, onAccept: () -> Unit, onDismiss: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    InfoCard("상세 안내를 켜 볼까요?", "최근 와이퍼 연습 두 번을 직접 중단했어요. 상세 설명을 켜면 단계별 안내를 볼 수 있어요.")
    Button(onClick = { confirm = true }, enabled = !busy) { Text("상세 안내 켜기") }
    OutlinedButton(onClick = onDismiss, enabled = !busy) { Text("나중에 보기") }
    OutlinedButton(onClick = onDismiss, enabled = !busy) { Text("닫기") }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text("상세 안내를 켤까요?") },
        text = { Text("안내 상세도만 변경해요.") },
        confirmButton = { TextButton(onClick = { confirm = false; onAccept() }) { Text("켜기") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("취소") } })
}

@Composable
private fun DoorStatusScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val reader = remember(context) { VehicleDoorReader(context) }
    var result by remember { mutableStateOf("운전석 문 상태를 아직 조회하지 않았어요.") }

    DisposableEffect(reader) {
        onDispose { reader.close() }
    }

    ScreenTitle("운전석 문 상태", "주차 중 조회 버튼을 눌러 상태를 확인해요.")
    if (!VehicleSdkAccess.enabled) Text(androidx.compose.ui.res.stringResource(R.string.vehicle_safety_blocked))
    InfoCard("문 열림·닫힘", result)
    Text("마지막으로 조회한 값이에요. 잠금 상태는 확인하지 않으며, 현재 상태와 다를 수 있어요.")
    Button(onClick = { reader.read { result = it } }, enabled = VehicleSdkAccess.enabled) { Text("문 상태 조회") }
    BackButton(onClick = onBack)
}

private fun TextStyle.enlarged(scale: Float): TextStyle = copy(
    fontSize = if (fontSize.isSpecified) fontSize * scale else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * scale else lineHeight,
)

private fun Typography.enlarged(scale: Float): Typography = copy(
    displayLarge = displayLarge.enlarged(scale),
    displayMedium = displayMedium.enlarged(scale),
    displaySmall = displaySmall.enlarged(scale),
    headlineLarge = headlineLarge.enlarged(scale),
    headlineMedium = headlineMedium.enlarged(scale),
    headlineSmall = headlineSmall.enlarged(scale),
    titleLarge = titleLarge.enlarged(scale),
    titleMedium = titleMedium.enlarged(scale),
    titleSmall = titleSmall.enlarged(scale),
    bodyLarge = bodyLarge.enlarged(scale),
    bodyMedium = bodyMedium.enlarged(scale),
    bodySmall = bodySmall.enlarged(scale),
    labelLarge = labelLarge.enlarged(scale),
    labelMedium = labelMedium.enlarged(scale),
    labelSmall = labelSmall.enlarged(scale),
)

@Composable
private fun ScreenTitle(title: String, description: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Text(description, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun InfoCard(title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun NavButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.sizeIn(minHeight = com.example.hyundiaboysranger.ui.theme.DesignTokens.touchMinimum)) { Text(label) }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.sizeIn(minHeight = com.example.hyundiaboysranger.ui.theme.DesignTokens.touchMinimum)) { Text(androidx.compose.ui.res.stringResource(R.string.checklist_home)) }
}

internal enum class HomeDestination(val key: String, val title: Int) {
    CHECKLIST("checklist", R.string.home_checklist), DOOR("door", R.string.home_door),
    ROUTE("route", R.string.home_route), LEARN("learn", R.string.home_learn),
    WARNINGS("warnings", R.string.home_warnings), CATALOG("warning_catalog", R.string.warning_catalog_title),
    REVIEW("review", R.string.home_review), PROFILE("profile", R.string.home_profile), SETTINGS("settings", R.string.home_settings)
}

@Composable
private fun HomeActions(onNavigate: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeDestination.entries.forEach { destination ->
            NavButton(androidx.compose.ui.res.stringResource(destination.title)) { onNavigate(destination.key) }
        }
    }
}
