package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun LocalSettingsBackupScreen() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val repository = remember { LocalSettingsBackupRepository(context) }
    val current = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var backup by remember { mutableStateOf(repository.load()) }
    var writeUnknown by remember { mutableStateOf(repository.isWriteUnknown()) }
    var unreadableBackup by remember { mutableStateOf(repository.hasStoredData() && backup == null) }
    var pendingSave by remember { mutableStateOf<UserSettings?>(null) }
    var pendingRestore by remember { mutableStateOf<LocalSettingsBackup?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    fun save(settings: UserSettings) {
        busy = true
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                if (current.loadIfReadable() == null || (repository.hasStoredData() && repository.load() == null)) false
                else repository.save(settings)
            }
            if (success) {
                backup = repository.load()
                unreadableBackup = repository.hasStoredData() && backup == null
            }
            writeUnknown = repository.isWriteUnknown()
            message = resources.getString(if (success) R.string.backup_saved else R.string.backup_write_unconfirmed)
            busy = false
        }
    }
    Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.backup_details))
    if (writeUnknown) {
        Text(stringResource(R.string.backup_unknown))
        OutlinedButton(onClick = {
            busy = true
            scope.launch {
                val success = withContext(Dispatchers.IO) { repository.retryWrite() }
                writeUnknown = repository.isWriteUnknown()
                if (success) { backup = repository.load(); unreadableBackup = false }
                message = resources.getString(if (success) R.string.backup_retry_success else R.string.backup_retry_unconfirmed)
                busy = false
            }
        }, enabled = !busy) { Text(stringResource(R.string.backup_retry)) }
    }
    backup?.let { Text(stringResource(R.string.backup_timestamp, DateFormat.getDateTimeInstance().format(Date(it.savedAt)))) }
        ?: Text(stringResource(if (unreadableBackup) R.string.backup_unreadable else R.string.backup_empty))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = {
            val settings = current.loadIfReadable()
            if (settings == null) message = resources.getString(R.string.backup_settings_unreadable)
            else if (backup == null) save(settings) else pendingSave = settings
        }, enabled = !busy && !unreadableBackup && !writeUnknown) { Text(stringResource(if (busy) R.string.backup_saving else R.string.backup_save)) }
        OutlinedButton(onClick = { pendingRestore = repository.load() }, enabled = backup != null && !busy && !writeUnknown) { Text(stringResource(R.string.backup_restore)) }
    }
    if (message.isNotEmpty()) Text(message)
    pendingSave?.let { captured ->
        AlertDialog(onDismissRequest = { pendingSave = null }, title = { Text(stringResource(R.string.backup_replace_title)) },
            text = { Text(stringResource(R.string.backup_replace_details)) },
            confirmButton = { TextButton(onClick = { pendingSave = null; save(captured) }) { Text(stringResource(R.string.backup_replace_confirm)) } },
            dismissButton = { TextButton(onClick = { pendingSave = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
    pendingRestore?.let { saved ->
        val settings = saved.settings
        AlertDialog(onDismissRequest = { pendingRestore = null }, title = { Text(stringResource(R.string.backup_restore_title)) },
            text = { Text(stringResource(R.string.backup_restore_details, settings.profile.label, settings.effectiveTextSizePercent,
                settings.displayMode.label, stringResource(if (settings.highContrast) R.string.backup_on else R.string.backup_off),
                stringResource(if (settings.detailedGuidance) R.string.backup_detailed else R.string.backup_simple))) },
            confirmButton = { TextButton(onClick = {
                pendingRestore = null; busy = true
                scope.launch {
                    val applied = withContext(Dispatchers.IO) { current.save(settings, "내부 백업 복원") }
                    message = resources.getString(if (applied) R.string.backup_restore_saved else R.string.backup_restore_unconfirmed)
                    busy = false
                }
            }) { Text(stringResource(R.string.backup_restore_apply)) } },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
}
