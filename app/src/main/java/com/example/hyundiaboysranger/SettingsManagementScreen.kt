package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hyundiaboysranger.ui.theme.DesignTokens

internal enum class SettingsResetEvent { REQUEST, CANCEL, CONFIRM }
internal fun resetConfirmationVisible(visible: Boolean, event: SettingsResetEvent): Boolean = when (event) {
    SettingsResetEvent.REQUEST -> true
    SettingsResetEvent.CANCEL, SettingsResetEvent.CONFIRM -> false
}

@Composable
internal fun SettingsResetControl(onReset: () -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { confirmReset = resetConfirmationVisible(confirmReset, SettingsResetEvent.REQUEST) },
        modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.settings_reset)) }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = resetConfirmationVisible(confirmReset, SettingsResetEvent.CANCEL) },
        title = { Text(stringResource(R.string.settings_reset_title)) },
        text = { Text(stringResource(R.string.settings_reset_details)) },
        confirmButton = { TextButton(onClick = { confirmReset = resetConfirmationVisible(confirmReset, SettingsResetEvent.CONFIRM); onReset() }) { Text(stringResource(R.string.settings_reset_confirm)) } },
        dismissButton = { TextButton(onClick = { confirmReset = resetConfirmationVisible(confirmReset, SettingsResetEvent.CANCEL) }) { Text(stringResource(R.string.action_cancel)) } })
}

@Composable
fun SettingsManagement(onReset: () -> Unit) {
    val transfer = LocalSettingsTransfer.current
    LocalSettingsBackupScreen()
    Text(stringResource(R.string.settings_transfer_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.settings_transfer_fallback))
    Text(transfer.message)
    if (!transfer.canExport) Text(stringResource(R.string.settings_export_unavailable))
    if (!transfer.canImport) Text(stringResource(R.string.settings_import_unavailable))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = transfer.export, enabled = !transfer.busy && transfer.canExport, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.settings_export)) }
        OutlinedButton(onClick = transfer.import, enabled = !transfer.busy && transfer.canImport, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.settings_import)) }
    }
    transfer.pending?.let { imported ->
        Text(stringResource(R.string.settings_restore_preview, imported.profile.label, imported.effectiveTextSizePercent, imported.displayMode.label))
        Text(stringResource(R.string.settings_restore_details))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = transfer.confirm, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.settings_restore)) }
            OutlinedButton(onClick = transfer.cancel, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.settings_restore_cancel)) }
        }
    }
    SettingsResetControl(onReset)
}
