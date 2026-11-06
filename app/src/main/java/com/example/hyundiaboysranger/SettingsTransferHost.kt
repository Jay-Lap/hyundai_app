package com.example.hyundiaboysranger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsTransferControl(
    val message: String = "화면·사용자·경로 선호만 파일로 저장해요. 복습 기록과 차량 데이터는 포함하지 않아요.",
    val pending: UserSettings? = null,
    val busy: Boolean = false,
    val export: () -> Unit = {}, val import: () -> Unit = {}, val confirm: () -> Unit = {}, val cancel: () -> Unit = {},
)
val LocalSettingsTransfer = staticCompositionLocalOf { SettingsTransferControl() }

@Composable
fun SettingsTransferHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("화면·사용자·경로 선호를 파일로 저장하고 복원해요. 복습 기록은 포함하지 않아요.") }
    var pending by remember { mutableStateOf<UserSettings?>(null) }
    var busy by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) { busy = false; message = "설정 내보내기를 취소했어요." }
        else scope.launch {
            val data = SettingsBackup.encode(SettingsRepository(context).load())
            val success = withContext(Dispatchers.IO) {
                try { context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(data.toByteArray(Charsets.UTF_8)) } != null }
                catch (_: Exception) { false }
            }
            busy = false; message = if (success) "설정 파일을 저장했어요." else "파일을 저장하지 못했어요. 저장 위치를 확인해 주세요."
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) { busy = false; message = "설정 가져오기를 취소했어요." }
        else scope.launch {
            val decoded = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readNBytes(4097)
                        if (bytes.size > 4096) null else SettingsBackup.decode(bytes.toString(Charsets.UTF_8))
                    }
                } catch (_: Exception) { null }
            }
            pending = decoded; busy = false
            message = if (decoded == null) "유효한 설정 파일이 아니에요. 현재 설정은 유지했어요." else "파일을 확인했어요. 설정 화면에서 복원을 확정해 주세요."
        }
    }
    CompositionLocalProvider(LocalSettingsTransfer provides SettingsTransferControl(message, pending, busy,
        export = {
            if (!busy) { busy = true
                try { export.launch("pleos-assist-settings.txt") } catch (_: Exception) { busy = false; message = "파일 선택기를 실행할 수 없어요." }
            }
        }, import = {
            if (!busy) { busy = true; pending = null
                try { import.launch(arrayOf("text/plain", "application/octet-stream")) } catch (_: Exception) { busy = false; message = "파일 선택기를 실행할 수 없어요." }
            }
        }, confirm = {
            pending?.let { SettingsRepository(context).save(it) }
            pending = null; message = "설정을 복원했어요."
        }, cancel = { pending = null; message = "복원을 취소하고 현재 설정을 유지했어요." }
    )) { content() }
}
