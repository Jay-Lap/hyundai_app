package com.example.hyundiaboysranger

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceTextSizeControl(val available: Boolean, val message: String, val launch: () -> Unit)
val LocalVoiceTextSize = staticCompositionLocalOf {
    VoiceTextSizeControl(false, "이 기기에는 음성 인식 서비스가 없어요. 슬라이더를 이용해 주세요.") {}
}

/** Owned above the parking gate: opening the recognizer pauses the vehicle Activity. */
@Composable
fun VoiceTextSizeHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var message by rememberSaveableMessage(resources.getString(R.string.voice_initial))
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, resources.getString(R.string.voice_prompt))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
    val available = intent.resolveActivity(context.packageManager) != null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        val percent = spoken?.let(::parseTextSizeVoice)
        when {
            result.resultCode != Activity.RESULT_OK -> message = resources.getString(R.string.voice_cancelled)
            percent == null -> message = resources.getString(R.string.voice_invalid)
            else -> {
                message = resources.getString(R.string.voice_saving)
                scope.launch {
                    val success = withContext(Dispatchers.IO) {
                        val repository = SettingsRepository(context)
                        repository.loadIfReadable()?.let { repository.save(it.withTextSize(percent)) } == true
                    }
                    message = if (success) resources.getString(R.string.voice_saved, percent)
                    else resources.getString(R.string.voice_write_unknown)
                }
            }
        }
    }
    CompositionLocalProvider(LocalVoiceTextSize provides VoiceTextSizeControl(available,
        if (available) message else resources.getString(R.string.voice_unavailable)) {
        try { launcher.launch(intent) }
        catch (_: Exception) { message = resources.getString(R.string.voice_launch_failed) }
    }) { content() }
}

@Composable
private fun rememberSaveableMessage(initial: String) = androidx.compose.runtime.saveable.rememberSaveable {
    mutableStateOf(initial)
}
