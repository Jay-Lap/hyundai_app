package com.example.hyundiaboysranger

import androidx.compose.material3.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.hyundiaboysranger.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class WarningCardState(val entry: WarningEntry, val saved: Boolean, val busy: Boolean, val storageNormal: Boolean) {
    val canAdd: Boolean get() = !busy && storageNormal && !saved
}
internal sealed interface WarningCardEvent { data object Add : WarningCardEvent }

@Composable
internal fun WarningCard(state: WarningCardState, onEvent: (WarningCardEvent) -> Unit) {
    val entry = state.entry
    val buttonText = stringResource(if (state.saved) R.string.warning_saved else R.string.warning_add)
    val accessibilityName = stringResource(if (state.saved) R.string.warning_saved_accessibility else R.string.warning_add_accessibility, entry.title)
    Text(entry.title, style = MaterialTheme.typography.titleLarge)
    Text(entry.description)
    Text(entry.action)
    Text(stringResource(R.string.warning_catalog_manual))
    OutlinedButton(onClick = { onEvent(WarningCardEvent.Add) }, enabled = state.canAdd,
        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = DesignTokens.touchMinimum).semantics { contentDescription = accessibilityName }) {
        Text(buttonText)
    }
}

@Composable
fun WarningCatalogScreen(onBack: () -> Unit, focusId: String? = null, reviewRepository: ReviewRepository? = null) {
    val resources = LocalResources.current
    val context = LocalContext.current
    val repository = remember(context, reviewRepository) { reviewRepository ?: ReviewRepository(context) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(repository.storageStatus()) }
    var consent by remember { mutableStateOf(repository.consent) }
    var saved by remember { mutableStateOf(repository.items()) }
    var pendingConsent by remember { mutableStateOf<WarningEntry?>(null) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val entries = remember(resources) {
        runCatching { resources.openRawResource(R.raw.warning_catalog).bufferedReader().use { parseWarningCatalog(it.readText()) } }.getOrDefault(emptyList())
    }
    Text(stringResource(R.string.warning_catalog_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.warning_catalog_notice))
    ReviewRecoverySection(repository, status) {
        status = repository.storageStatus()
        if (status == ReviewStorageStatus.NORMAL) { consent = repository.consent; saved = repository.items() }
    }
    if (message.isNotEmpty()) Text(message)
    if (entries.isEmpty()) Text(stringResource(R.string.warning_catalog_unavailable))
    if (focusId != null && entries.none { it.id == focusId }) Text(stringResource(R.string.warning_missing_item))
    entries.filter { focusId == null || it.id == focusId }.forEach { entry ->
        val alreadySaved = saved.any { it.kind == "warning" && it.id == entry.id }
        WarningCard(WarningCardState(entry, alreadySaved, busy, status == ReviewStorageStatus.NORMAL)) {
            if (!consent) pendingConsent = entry else {
                busy = true
                scope.launch {
                    val success = withContext(Dispatchers.IO) { repository.addWarning(entry.id) }
                    status = repository.storageStatus()
                    if (success) saved = repository.items()
                    message = if (success) "${entry.title} 학습 항목을 복습에 추가했어요." else "추가 결과를 확인해 주세요. 저장 상태를 확인하거나 다시 시도할 수 있어요."
                    busy = false
                }
            }
        }
    }
    pendingConsent?.let { entry ->
        AlertDialog(onDismissRequest = { pendingConsent = null },
            title = { Text("복습 기록을 저장할까요?") },
            text = { Text("복습 저장이 꺼져 있어요. 동의하면 ${entry.title} 학습 항목을 이 기기에 저장해요. 차량의 경고등 감지 기록이 아니에요.") },
            confirmButton = { TextButton(onClick = {
                pendingConsent = null; busy = true
                scope.launch {
                    val success = withContext(Dispatchers.IO) { repository.setConsent(true) && repository.addWarning(entry.id) }
                    status = repository.storageStatus()
                    if (status == ReviewStorageStatus.NORMAL) { consent = repository.consent; saved = repository.items() }
                    message = if (success) "${entry.title} 학습 항목을 복습에 추가했어요." else "추가 결과를 확인해 주세요. 동의와 저장 상태를 확인할 수 있어요."
                    busy = false
                }
            }) { Text("동의하고 추가") } },
            dismissButton = { TextButton(onClick = { pendingConsent = null }) { Text("취소") } })
    }
    OutlinedButton(onClick = onBack) { Text(stringResource(R.string.warning_catalog_back)) }
}
