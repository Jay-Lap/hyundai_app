package com.example.hyundiaboysranger

import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.hyundiaboysranger.ui.theme.HyundiaBoysRangerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HyundiaBoysRangerTheme {
                Surface(modifier = Modifier.fillMaxSize()) { DrivingHelperApp() }
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

    val systemDensity = LocalDensity.current
    val textScale = if (settings.largeTitle) 1.25f else 1f
    CompositionLocalProvider(
        LocalDensity provides Density(systemDensity.density, systemDensity.fontScale * textScale)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "안전 운전 도우미",
                style = MaterialTheme.typography.headlineLarge
            )
            Text("화면 시안 · 차량과 내비게이션이 연결되지 않았어요", style = MaterialTheme.typography.bodyLarge)

            when (screen) {
                "home" -> {
                    InfoCard("현재 사용자", "${settings.profile.label} · 사용자 유형과 글자 크기를 기기에 저장해요.")
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
                "route" -> {
                    ScreenTitle("경로 준비", "출발 전에 경로와 운전 부담 요소를 살펴보는 공간이에요.")
                    InfoCard("경로 정보 없음", "목적지 검색과 경로 비교는 내비게이션 연동 후 사용할 수 있어요.")
                    Text("큰길 선호와 고속도로 선호는 별도로 설정할 예정이에요.")
                    BackButton { screen = "home" }
                }
                "learn" -> {
                    ScreenTitle("차량 조작 배우기", "공조, 김서림 제거와 와이퍼 조작을 연습할 수 있어요.")
                    InfoCard("실습 잠김", "주차 상태와 차량 정보 확인 기능을 연결한 뒤 사용할 수 있어요.")
                    BackButton { screen = "home" }
                }
                "warnings" -> {
                    ScreenTitle("경고등 도움말", "차량이 보내는 경고의 의미와 대응 방법을 확인하는 공간이에요.")
                    InfoCard("차량 정보 미연결", "실제 경고를 확인하고 있지 않아요. 차량의 경고 표시와 공식 매뉴얼을 확인해 주세요.")
                    BackButton { screen = "home" }
                }
                "review" -> {
                    ScreenTitle("내 복습", "어려웠던 조작과 완료하지 못한 연습을 다시 살펴봐요.")
                    InfoCard("기록 없음", "복습 기록 저장과 복원은 아직 연결되지 않았어요.")
                    BackButton { screen = "home" }
                }
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
