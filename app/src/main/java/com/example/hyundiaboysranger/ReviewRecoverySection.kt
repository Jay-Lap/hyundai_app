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
            val operation = repository.pendingWriteDescription() ?: "복습 작업"
            Text("${operation}의 저장 결과를 확인하지 못했어요. 이미 저장되었을 수도 있어요. 화면의 값으로 저장 여부를 알 수 없어요.")
            Text("다시 시도하지 않아도 이전 작업이 취소되거나 원래 기록이 복구되는 것은 아니에요. 앱을 완전히 종료한 후 다시 열면 남아 있는 내용을 확인할 수 있지만, 이전 작업의 성공 여부는 알 수 없고 내용을 읽지 못할 수도 있어요.")
            OutlinedButton(onClick = { confirmRetry = true }, enabled = !resetting) { Text("같은 복습 작업 다시 시도") }
            if (confirmRetry) AlertDialog(
                onDismissRequest = { confirmRetry = false },
                title = { Text("같은 작업을 다시 시도할까요?") },
                text = { Text("다시 시도할 작업: $operation\n삭제나 초기화 작업이라면 기록이 삭제될 수 있어요. 취소해도 이전 시도가 취소되거나 복구되지는 않아요.") },
                confirmButton = { TextButton(onClick = {
                    confirmRetry = false; resetting = true
                    scope.launch {
                        withContext(Dispatchers.IO) { repository.retryWrite() }
                        resetting = false
                        onChanged()
                    }
                }) { Text("다시 시도") } },
                dismissButton = { TextButton(onClick = { confirmRetry = false }) { Text("취소") } },
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
