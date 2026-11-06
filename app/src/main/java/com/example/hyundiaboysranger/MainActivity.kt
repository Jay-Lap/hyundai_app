package com.example.hyundiaboysranger

import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.example.hyundiaboysranger.ui.theme.HyundiaBoysRangerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val speedPermission = "android.car.permission.CAR_SPEED"
        val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
        if (checkSelfPermission(speedPermission) != PackageManager.PERMISSION_GRANTED) permissionRequest.launch(speedPermission)
        setContent {
            HyundiaBoysRangerTheme {
                Surface(modifier = Modifier.fillMaxSize()) { ParkedApp() }
            }
        }
    }
}

@Composable
fun DrivingHelperApp() {
    var screen by rememberSaveable { mutableStateOf("home") }
    val context = LocalContext.current
    val settingsRepository = remember(context) { SettingsRepository(context) }
    var settings by remember(settingsRepository) { mutableStateOf(settingsRepository.load()) }

    fun updateSettings(updated: UserSettings) {
        settings = updated
        settingsRepository.save(updated)
    }

    val baseTypography = MaterialTheme.typography
    val appTypography = remember(baseTypography, settings.largeTitle) {
        if (settings.largeTitle) baseTypography.enlarged() else baseTypography
    }
    MaterialTheme(typography = appTypography) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "안전 운전 도우미",
                style = MaterialTheme.typography.headlineLarge
            )
            Text("내비게이션 기능은 아직 연결되지 않았어요", style = MaterialTheme.typography.bodyLarge)

            when (screen) {
                "home" -> {
                    InfoCard("현재 사용자", "${settings.profile.label} · 사용자 유형과 글자 크기를 기기에 저장해요.")
                    val pendingReview = remember { ReviewRepository(context).load() }
                    if (pendingReview?.complete == false) {
                        InfoCard("다시 연습해 볼까요?", "완료하지 못한 와이퍼 연습이 있어요.")
                        NavButton("와이퍼 복습 시작") { screen = "learn" }
                    }
                    NavButton("운전석 문 상태") { screen = "door" }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NavButton("경로 준비") { screen = "route" }
                        NavButton("차량 조작 배우기") { screen = "learn" }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NavButton("경고등 도움말") { screen = "warnings" }
                        NavButton("내 복습") { screen = "review" }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NavButton("사용자 유형") { screen = "profile" }
                        NavButton("설정") { screen = "settings" }
                    }
                }
                "profile" -> {
                    ScreenTitle("사용자 유형", "고령자는 큰 글자, 초보자는 보통 글자를 먼저 적용해요. 다른 기능은 준비 중이에요.")
                    UserProfile.entries.forEach { option ->
                        OutlinedButton(onClick = { updateSettings(settings.withProfile(option)) }, modifier = Modifier.sizeIn(minHeight = 56.dp)) {
                            Text(if (settings.profile == option) "${option.label} · 선택됨" else option.label)
                        }
                    }
                    BackButton { screen = "home" }
                }
                "door" -> DoorStatusScreen(onBack = { screen = "home" })
                "route" -> {
                    ScreenTitle("경로 준비", "선호를 저장하고 기본 내비게이션에서 목적지를 검색해요.")
                    Text("큰길 선호 · ${if (settings.preferWideRoads) "켜짐" else "꺼짐"}")
                    androidx.compose.material3.Switch(checked = settings.preferWideRoads, onCheckedChange = {
                        updateSettings(settings.copy(preferWideRoads = it))
                    })
                    Text("고속도로 선호 · ${if (settings.preferHighways) "켜짐" else "꺼짐"}")
                    androidx.compose.material3.Switch(checked = settings.preferHighways, onCheckedChange = {
                        updateSettings(settings.copy(preferHighways = it))
                    })
                    InfoCard("경로 계산 연동 준비 중", "두 선호는 별도로 저장돼요. 아직 기본 내비게이션의 경로에는 적용되지 않아요.")
                    var launchMessage by remember { mutableStateOf("") }
                    Button(onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage("ai.umos.maps.android.navigation.app")
                        launchMessage = try {
                            if (intent == null) "기본 내비게이션 실행 화면을 찾지 못했어요. 차량 홈에서 열어 주세요."
                            else { context.startActivity(intent); "" }
                        } catch (_: Exception) { "내비게이션을 열 수 없어요. 차량 홈에서 실행해 주세요." }
                    }) { Text("기본 내비게이션 열기") }
                    if (launchMessage.isNotEmpty()) Text(launchMessage)
                    BackButton { screen = "home" }
                }
                "learn" -> PracticeScreen(onBack = { screen = "home" })
                "warnings" -> {
                    ScreenTitle("경고등 도움말", "차량이 보내는 경고의 의미와 대응 방법을 확인하는 공간이에요.")
                    InfoCard("차량 정보 미연결", "실제 경고를 확인하고 있지 않아요. 차량의 경고 표시와 공식 매뉴얼을 확인해 주세요.")
                    BackButton { screen = "home" }
                }
                "review" -> ReviewScreen(onPractice = { screen = "learn" }, onBack = { screen = "home" })
                "settings" -> {
                    ScreenTitle("설정", "앱 전체에 표시되는 글자 크기를 바꿀 수 있어요.")
                    Text("글자 크기 · ${if (settings.largeTitle) "크게" else "보통"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { updateSettings(settings.copy(largeTitle = false)) }) { Text("보통") }
                        Button(onClick = { updateSettings(settings.copy(largeTitle = true)) }) { Text("크게") }
                    }
                    InfoCard("준비 중", "화면 대비와 테마, 음성 안내, 주행 보조 설정은 추후 연결할 예정이에요.")
                    Text("사용자 유형과 글자 크기는 이 기기에 저장돼요.")
                    BackButton { screen = "home" }
                }
            }
        }
    }
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
    InfoCard("문 열림·닫힘", result)
    Text("마지막으로 조회한 값이에요. 잠금 상태는 확인하지 않으며, 현재 상태와 다를 수 있어요.")
    Button(onClick = { reader.read { result = it } }) { Text("문 상태 조회") }
    BackButton(onClick = onBack)
}

private fun TextStyle.enlarged(): TextStyle = copy(
    fontSize = if (fontSize.isSpecified) fontSize * 1.4f else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * 1.4f else lineHeight,
)

private fun Typography.enlarged(): Typography = copy(
    displayLarge = displayLarge.enlarged(),
    displayMedium = displayMedium.enlarged(),
    displaySmall = displaySmall.enlarged(),
    headlineLarge = headlineLarge.enlarged(),
    headlineMedium = headlineMedium.enlarged(),
    headlineSmall = headlineSmall.enlarged(),
    titleLarge = titleLarge.enlarged(),
    titleMedium = titleMedium.enlarged(),
    titleSmall = titleSmall.enlarged(),
    bodyLarge = bodyLarge.enlarged(),
    bodyMedium = bodyMedium.enlarged(),
    bodySmall = bodySmall.enlarged(),
    labelLarge = labelLarge.enlarged(),
    labelMedium = labelMedium.enlarged(),
    labelSmall = labelSmall.enlarged(),
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
    Button(onClick = onClick, modifier = Modifier.sizeIn(minHeight = 56.dp)) { Text(label) }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.sizeIn(minHeight = 56.dp)) { Text("홈으로") }
}
