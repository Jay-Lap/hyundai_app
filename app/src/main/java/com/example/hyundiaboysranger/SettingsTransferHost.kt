package com.example.hyundiaboysranger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsTransferControl(
    val message: String = "화면·사용자·경로 선호만 파일로 저장해요. 복습 기록과 차량 데이터는 포함하지 않아요.",
    val pending: UserSettings? = null,
    val busy: Boolean = false,
    val canExport: Boolean = true,
    val canImport: Boolean = true,
    val export: () -> Unit = {}, val import: () -> Unit = {}, val confirm: () -> Unit = {}, val cancel: () -> Unit = {},
)
val LocalSettingsTransfer = staticCompositionLocalOf { SettingsTransferControl() }

@Composable
fun SettingsTransferHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val createContract = remember { ActivityResultContracts.CreateDocument("text/plain") }
    val openContract = remember { ActivityResultContracts.OpenDocument() }
    var canExport by remember { mutableStateOf(false) }
    var canImport by remember { mutableStateOf(false) }
    DisposableEffect(context, lifecycleOwner) {
        fun refreshAvailability() {
            val manager = context.packageManager
            canExport = createContract.createIntent(context, "pleos-assist-settings.txt").resolveActivity(manager) != null
            canImport = openContract.createIntent(context, arrayOf("text/plain", "application/octet-stream")).resolveActivity(manager) != null
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshAvailability()
        }
        refreshAvailability()
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var message by remember { mutableStateOf("화면·사용자·경로 선호를 파일로 저장하고 복원해요. 복습 기록은 포함하지 않아요.") }
    var pending by remember { mutableStateOf<UserSettings?>(null) }
    var busy by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(createContract) { uri ->
        if (uri == null) { busy = false; message = "파일 저장이 취소되었거나 선택 화면을 사용할 수 없어요. 내부 백업을 이용할 수 있어요." }
        else scope.launch {
            val current = SettingsRepository(context).loadIfReadable()
            if (current == null) {
                busy = false; message = "현재 설정을 확인할 수 없어 파일에 저장하지 않았어요."
                return@launch
            }
            val data = SettingsBackup.encode(current)
            val success = withContext(Dispatchers.IO) {
                try { context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(data.toByteArray(Charsets.UTF_8)) } != null }
                catch (_: Exception) { false }
            }
            busy = false; message = if (success) "파일 쓰기를 마쳤어요. 저장 위치에서 파일을 확인해 주세요." else "파일 쓰기를 확인하지 못했어요. 저장 위치의 파일을 확인해 주세요."
        }
    }
    val import = rememberLauncherForActivityResult(openContract) { uri ->
        if (uri == null) { busy = false; message = "파일 가져오기가 취소되었거나 선택 화면을 사용할 수 없어요." }
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
    CompositionLocalProvider(LocalSettingsTransfer provides SettingsTransferControl(message, pending, busy, canExport, canImport,
        export = {
            if (!busy && canExport && SettingsRepository(context).status().writable) { busy = true
                try { export.launch("pleos-assist-settings.txt") } catch (_: Exception) { busy = false; message = "파일 선택기를 실행할 수 없어요." }
            }
        }, import = {
            if (!busy && canImport && SettingsRepository(context).status().writable) { busy = true; pending = null
                try { import.launch(arrayOf("text/plain", "application/octet-stream")) } catch (_: Exception) { busy = false; message = "파일 선택기를 실행할 수 없어요." }
            }
        }, confirm = {
            val captured = pending
            pending = null; busy = true
            scope.launch {
                val applied = withContext(Dispatchers.IO) { captured?.let { SettingsRepository(context).save(it, "설정 파일 복원") } == true }
                message = if (applied) "설정을 복원했어요." else "복원 결과를 확인할 수 없어요. 설정 화면에서 상태를 확인해 주세요."
                busy = false
            }
        }, cancel = { pending = null; message = "복원을 취소하고 현재 설정을 유지했어요." }
    )) { content() }
}
