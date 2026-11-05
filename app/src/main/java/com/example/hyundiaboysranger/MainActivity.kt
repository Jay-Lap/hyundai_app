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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hyundiaboysranger.ui.theme.HyundiaBoysRangerTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HyundiaBoysRangerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DrivingHelperApp()
                }
            }
        }
    }
}

@Composable
fun DrivingHelperApp() {
    var currentScreen by rememberSaveable { mutableStateOf("home") }
    var isLargeText by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "안전 운전 도우미",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = if (isLargeText) 42.sp else 32.sp
        )

        when (currentScreen) {
            "home" -> {
                Text(
                    text = "편안한 운전을 위한 안내를 준비해요.",
                    style = MaterialTheme.typography.titleLarge
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "차량 연결",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "차량 정보 연결은 아직 준비 중이에요.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { currentScreen = "driving" }) {
                        Text("주행 안내")
                    }

                    OutlinedButton(onClick = { currentScreen = "settings" }) {
                        Text("설정")
                    }
                }
            }

            "driving" -> {
                Text(
                    text = "주행 안내",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "주행 안내 기능을 준비하고 있어요.",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(onClick = { currentScreen = "home" }) {
                    Text("홈으로")
                }
            }

            "settings" -> {
                Text(
                    text = "설정",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "홈 화면 제목 크기",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = if (isLargeText) "현재: 크게" else "현재: 보통",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { isLargeText = false }) {
                        Text("보통")
                    }
                    Button(onClick = { isLargeText = true }) {
                        Text("크게")
                    }
                }
                OutlinedButton(onClick = { currentScreen = "home" }) {
                    Text("홈으로")
                }
            }
        }
    }
}
