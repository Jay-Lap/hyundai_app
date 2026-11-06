package com.example.hyundiaboysranger

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

class VoiceTextSizeControl(val available: Boolean, val message: String, val launch: () -> Unit)
val LocalVoiceTextSize = staticCompositionLocalOf {
    VoiceTextSizeControl(false, "이 기기에는 음성 인식 서비스가 없어요. 슬라이더를 이용해 주세요.") {}
}

/** Owned above the parking gate: opening the recognizer pauses the vehicle Activity. */
@Composable
fun VoiceTextSizeHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var message by rememberSaveableMessage()
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "글자 크기 150퍼센트라고 말해 주세요")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
    val available = intent.resolveActivity(context.packageManager) != null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        val percent = spoken?.let(::parseTextSizeVoice)
        message = when {
            result.resultCode != Activity.RESULT_OK -> "음성 입력을 취소했거나 인식하지 못했어요."
            percent == null -> "100부터 200퍼센트 사이의 크기를 하나만 말해 주세요."
            else -> {
                val repository = SettingsRepository(context)
                repository.save(repository.load().withTextSize(percent))
                "글자 크기를 ${percent}%로 저장했어요."
            }
        }
    }
    CompositionLocalProvider(LocalVoiceTextSize provides VoiceTextSizeControl(available,
        if (available) message else "이 기기에는 음성 인식 서비스가 없어요. 슬라이더를 이용해 주세요.") {
        try { launcher.launch(intent) }
        catch (_: Exception) { message = "음성 인식을 실행할 수 없어요. 슬라이더를 이용해 주세요." }
    }) { content() }
}

@Composable
private fun rememberSaveableMessage() = androidx.compose.runtime.saveable.rememberSaveable {
    mutableStateOf("음성 버튼을 누르고 ‘글자 크기 150퍼센트’라고 말해 보세요.")
}
