package com.example.hyundiaboysranger

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.hyundiaboysranger.ui.theme.HyundiaBoysRangerTheme

/** Debug-only visual review, limited to a non-automotive Android emulator. */
class PhonePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true)
        val automotive = packageManager.hasSystemFeature("android.hardware.type.automotive")
        setContent {
            HyundiaBoysRangerTheme {
                Surface(Modifier.fillMaxSize()) {
                    if (emulator && !automotive) VoiceTextSizeHost { SettingsTransferHost { DrivingHelperApp(phonePreview = true) } }
                    else Text("화면 확인용 모드는 일반 Android 휴대폰 에뮬레이터에서만 사용할 수 있어요.")
                }
            }
        }
    }
}
