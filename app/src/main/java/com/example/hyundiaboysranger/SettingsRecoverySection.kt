package com.example.hyundiaboysranger

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SettingsRecoverySection(repository: SettingsRepository, status: SettingsStorageStatus, onChanged: () -> Unit) {
    var snapshot by remember { mutableStateOf<SettingsRecoverySnapshot?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (status.kind == SettingsStorageKind.WRITE_UNKNOWN) {
        Text(stringResource(R.string.settings_write_unknown, repository.pendingWriteDescription() ?: stringResource(R.string.settings_operation)))
        OutlinedButton(onClick = {
            busy = true
            scope.launch {
                withContext(Dispatchers.IO) { repository.retryWrite() }
                busy = false
                onChanged()
            }
        }, enabled = !busy) { Text(stringResource(R.string.settings_retry_same)) }
        return
    }
    val message = when (status.kind) {
        SettingsStorageKind.WRONG_TYPE -> R.string.settings_wrong_type
        SettingsStorageKind.OUT_OF_RANGE -> R.string.settings_out_of_range
        SettingsStorageKind.MISSING -> R.string.settings_missing
        SettingsStorageKind.FUTURE_VERSION -> R.string.settings_future
        else -> return
    }
    Text(stringResource(message))
    if (status.kind == SettingsStorageKind.FUTURE_VERSION) return
    status.field?.let { Text(stringResource(R.string.settings_issue_field, it)) }
    OutlinedButton(onClick = { snapshot = repository.recoverySnapshot(); failed = snapshot == null }, enabled = !busy) {
        Text(stringResource(R.string.settings_show_original))
    }
    snapshot?.let { captured ->
        val text = remember(captured) {
            captured.values.entries.sortedBy { it.key }.joinToString("\n") { (key, value) ->
                "$key: ${value.toString().take(4097)}"
            }.take(4097)
        }
        val truncated = text.length > 4096
        Text(stringResource(R.string.settings_original_label))
        Text(text.take(4096))
        if (truncated) Text(stringResource(R.string.settings_original_too_large))
        OutlinedButton(onClick = { confirm = true }, enabled = !busy && !truncated) { Text(stringResource(R.string.settings_recovery_open)) }
    }
    if (failed) Text(stringResource(R.string.settings_recovery_failed))
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.settings_recovery_title)) },
        text = { Text(stringResource(R.string.settings_recovery_body)) },
        confirmButton = { TextButton(onClick = {
            val captured = snapshot
            confirm = false; busy = true
            scope.launch {
                val success = withContext(Dispatchers.IO) { captured != null && repository.resetDamagedAfterConfirmation(captured) }
                snapshot = null; busy = false; failed = !success
                onChanged()
            }
        }) { Text(stringResource(R.string.settings_recovery_confirm)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.settings_recovery_cancel)) } },
    )
}
