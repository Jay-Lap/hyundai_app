package com.example.hyundiaboysranger

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Never perform a reset until the user has opened the preview and confirmed it. */
@Composable
internal fun ReviewRecoverySection(repository: ReviewRepository, status: ReviewStorageStatus, onChanged: () -> Unit) {
    var snapshot by remember { mutableStateOf<ReviewRecoverySnapshot?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }
    var confirmRetry by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    when (status) {
        ReviewStorageStatus.NORMAL -> Unit
        ReviewStorageStatus.WRITE_UNKNOWN -> {
            val operation = repository.pendingWriteDescription() ?: stringResource(R.string.review_operation)
            Text(stringResource(R.string.review_write_unknown, operation))
            Text(stringResource(R.string.review_write_unknown_restart))
            OutlinedButton(onClick = { confirmRetry = true }, enabled = !resetting) { Text(stringResource(R.string.review_retry_same)) }
            if (confirmRetry) AlertDialog(
                onDismissRequest = { confirmRetry = false },
                title = { Text(stringResource(R.string.review_retry_title)) },
                text = { Text(stringResource(R.string.review_retry_details, operation)) },
                confirmButton = { TextButton(onClick = {
                    confirmRetry = false; resetting = true
                    scope.launch {
                        withContext(Dispatchers.IO) { repository.retryWrite() }
                        resetting = false
                        onChanged()
                    }
                }) { Text(stringResource(R.string.action_retry)) } },
                dismissButton = { TextButton(onClick = { confirmRetry = false }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
        ReviewStorageStatus.FUTURE_VERSION -> Text(stringResource(R.string.review_future_version))
        ReviewStorageStatus.DAMAGED -> {
            Text(stringResource(R.string.review_damaged))
            OutlinedButton(onClick = { snapshot = repository.recoverySnapshot(); failed = snapshot == null }, enabled = !resetting) {
                Text(stringResource(R.string.review_show_contents))
            }
            snapshot?.let { saved ->
                val text = remember(saved) {
                    saved.values.entries.sortedBy { it.key }.joinToString("\n") { (key, value) ->
                        "$key: ${value.toString().take(4097)}"
                    }.take(4097)
                }
                val truncated = text.length > 4096
                Text(stringResource(R.string.review_preview_label))
                Text(text.take(4096))
                if (truncated) Text(stringResource(R.string.review_preview_too_large))
                OutlinedButton(onClick = { confirm = true }, enabled = !truncated) {
                    Text(stringResource(R.string.review_reset_open))
                }
            }
            if (failed) Text(stringResource(R.string.review_reset_failed))
            if (confirm) AlertDialog(
                onDismissRequest = { confirm = false },
                title = { Text(stringResource(R.string.review_reset_title)) },
                text = { Text(stringResource(R.string.review_reset_explanation)) },
                confirmButton = { TextButton(onClick = {
                    val previous = snapshot
                    confirm = false; resetting = true
                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            previous != null && repository.resetDamagedAfterConfirmation(previous)
                        }
                        snapshot = null; failed = !success; resetting = false
                        onChanged()
                    }
                }) { Text(stringResource(R.string.review_reset_confirm)) } },
                dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.review_reset_cancel)) } },
            )
        }
    }
}
