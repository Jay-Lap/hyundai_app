package com.example.hyundiaboysranger

import android.os.Build
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.NonCancellable
import java.util.UUID
import java.text.DateFormat
import java.util.Date
import com.example.hyundiaboysranger.ui.theme.DesignTokens

private fun sdkSource(): String = if (Build.FINGERPRINT.contains("generic") ||
    Build.HARDWARE.contains("ranchu") || Build.MODEL.contains("sdk", ignoreCase = true))
    "EMU_SDK · 에뮬레이터" else "SDK · 실차 여부 미검증"

@Composable
fun PracticeScreen(detailed: Boolean = true, backLabelResource: Int = R.string.checklist_home, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val repository = remember { ReviewRepository(context) }
    val scope = rememberCoroutineScope()
    var consent by remember { mutableStateOf(repository.consent) }
    var reviewStatus by remember { mutableStateOf(repository.storageStatus()) }
    var reviewBusy by remember { mutableStateOf(false) }
    var reviewMessage by remember { mutableStateOf("") }
    var startWrite by remember { mutableStateOf<Deferred<Boolean>?>(null) }
    var attemptId by remember { mutableStateOf<String?>(null) }
    var stopping by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(PracticeProgress.READY) }
    var reading by remember { mutableStateOf<Boolean?>(null) }
    var readResult by remember { mutableStateOf<SignalResult<Boolean>?>(null) }
    val source = remember { sdkSource() }
    Text(stringResource(R.string.wiper_title), style = MaterialTheme.typography.headlineMedium)
    if (!VehicleSdkAccess.enabled) Text(stringResource(R.string.vehicle_safety_blocked))
    Text(stringResource(R.string.wiper_instructions))
    if (detailed) Text(stringResource(R.string.wiper_steps_detail))
    Text(stringResource(R.string.wiper_data_source, source))
    Text(stringResource(R.string.wiper_review_consent))
    ReviewRecoverySection(repository, reviewStatus) {
        reviewStatus = repository.storageStatus()
        if (reviewStatus == ReviewStorageStatus.NORMAL) consent = repository.consent
    }
    if (reviewMessage.isNotEmpty()) Text(reviewMessage)
    if (reviewStatus == ReviewStorageStatus.NORMAL) ReviewConsentControl(consent, !reviewBusy) { enabled ->
        reviewBusy = true
        val progressAtConsent = progress
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                val consentSaved = repository.setConsent(enabled)
                if (consentSaved && enabled && progressAtConsent != PracticeProgress.READY)
                    repository.save(progressAtConsent == PracticeProgress.COMPLETE, source)
                consentSaved
            }
            if (saved) consent = enabled
            reviewStatus = repository.storageStatus()
            reviewBusy = false
        }
    }
    if (progress != PracticeProgress.READY) {
        val step = when (progress) {
            PracticeProgress.WAIT_OFF -> 1
            PracticeProgress.WAIT_ON -> 2
            PracticeProgress.WAIT_FINAL_OFF, PracticeProgress.COMPLETE -> 3
            PracticeProgress.READY -> 1
        }
        Text(if (progress == PracticeProgress.COMPLETE) stringResource(R.string.wiper_completed_step) else stringResource(R.string.wiper_step, step))
        LinearProgressIndicator(progress = { if (progress == PracticeProgress.COMPLETE) 1f else (step - 1) / 3f })
        Text(stringResource(R.string.wiper_steps_summary))
    }
    Text(stringResource(when (progress) {
        PracticeProgress.READY -> R.string.wiper_progress_ready
        PracticeProgress.WAIT_OFF -> R.string.wiper_progress_off
        PracticeProgress.WAIT_ON -> R.string.wiper_progress_on
        PracticeProgress.WAIT_FINAL_OFF -> R.string.wiper_progress_final_off
        PracticeProgress.COMPLETE -> R.string.wiper_progress_complete
    }), style = MaterialTheme.typography.titleLarge)
    val readingText = stringResource(when (reading) { true -> R.string.wiper_running; false -> R.string.wiper_off; null -> R.string.wiper_unknown })
    Text(stringResource(R.string.wiper_reading, readingText))
    readResult?.let { if (it !is SignalResult.Value) Text(signalStatus(it)) }
    Text(stringResource(R.string.wiper_safety_notice))
    Button(onClick = {
        reading = null; readResult = null; progress = PracticeProgress.WAIT_OFF
        val id = UUID.randomUUID().toString()
        attemptId = id
        startWrite = if (consent) scope.async {
            val success = withContext(Dispatchers.IO) { repository.saveAttempt(id, "start", source) }
            reviewStatus = repository.storageStatus()
            reviewMessage = if (!success && reviewStatus == ReviewStorageStatus.NORMAL) resources.getString(R.string.review_wiper_start_failed) else ""
            success
        } else null
    }, enabled = VehicleSdkAccess.enabled && (progress == PracticeProgress.READY || progress == PracticeProgress.COMPLETE),
        modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) {
        Text(stringResource(if (progress == PracticeProgress.COMPLETE) R.string.wiper_restart else R.string.wiper_start))
    }
    val running = progress != PracticeProgress.READY && progress != PracticeProgress.COMPLETE
    LaunchedEffect(running) {
        if (running) {
            val reader = WiperReader(context)
            try {
                while (true) {
                    readResult = reader.read()
                    reading = readResult?.valueOrNull()
                    val next = WiperLesson.observe(progress, reading)
                    if (next == PracticeProgress.COMPLETE) {
                        stopping = true
                        if (consent) {
                            startWrite?.await()
                            val id = attemptId
                            val tagged = startWrite != null
                            val success = withContext(Dispatchers.IO) {
                                if (tagged && id != null) {
                                    val paired = repository.saveAttempt(id, "complete", source)
                                    if (!paired && repository.storageStatus() == ReviewStorageStatus.NORMAL) repository.save(true, source) else paired
                                } else repository.save(true, source)
                            }
                            reviewStatus = repository.storageStatus()
                            reviewMessage = if (!success && reviewStatus == ReviewStorageStatus.NORMAL) resources.getString(R.string.review_wiper_complete_write_failed) else ""
                        }
                        progress = PracticeProgress.COMPLETE
                        stopping = false
                        break
                    }
                    progress = next
                    delay(500)
                }
            } finally { withContext(NonCancellable + Dispatchers.IO) { reader.close() } }
        }
    }
    OutlinedButton(onClick = {
        if (!running || !consent || startWrite == null) onBack()
        else {
            stopping = true
            scope.launch {
                startWrite?.await()
                val id = attemptId
                if (id != null) withContext(Dispatchers.IO) { repository.saveAttempt(id, "stop", source) }
                onBack()
            }
        }
    }, enabled = !stopping, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(if (running) R.string.wiper_stop else backLabelResource)) }
    Text(stringResource(R.string.other_lessons_title), style = MaterialTheme.typography.titleLarge)
    ExtraLesson.entries.forEach { lesson -> ExtraLessonGuide(lesson) }
}

@Composable
internal fun ExtraLessonGuide(lesson: ExtraLesson, initiallyExpanded: Boolean = false) {
    var state by remember(lesson, initiallyExpanded) { mutableStateOf(ExtraLessonUiState(expanded = initiallyExpanded)) }
    OutlinedButton(onClick = { state = reduceExtraLesson(state, ExtraLessonUiEvent.Toggle) },
        modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) {
        Text(stringResource(if (state.expanded) R.string.lesson_close_guide else R.string.lesson_open_guide, stringResource(lesson.titleResource)))
    }
    if (state.expanded) {
        Text(stringResource(lesson.explanationResource))
        Text(stringResource(R.string.lesson_signal_unverified))
        val completionName = stringResource(R.string.lesson_completion_accessibility, stringResource(lesson.titleResource))
        OutlinedButton(onClick = {}, enabled = state.canConfirmCompletion, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum).semantics { contentDescription = completionName }) {
            Text(stringResource(R.string.lesson_completion_locked))
        }
    }
}

internal data class SavedWarningUiState(val title: String, val updatedAt: String, val busy: Boolean)
internal enum class SavedWarningUiEvent { OPEN, DELETE }
internal enum class ReviewDeleteEvent { CONFIRM, CANCEL }

@Composable
internal fun ReviewDeleteDialog(title: String, description: String, onEvent: (ReviewDeleteEvent) -> Unit) {
    AlertDialog(onDismissRequest = { onEvent(ReviewDeleteEvent.CANCEL) }, title = { Text(title) }, text = { Text(description) },
        confirmButton = { TextButton(onClick = { onEvent(ReviewDeleteEvent.CONFIRM) }) { Text(stringResource(R.string.review_delete_action)) } },
        dismissButton = { TextButton(onClick = { onEvent(ReviewDeleteEvent.CANCEL) }) { Text(stringResource(R.string.action_cancel)) } })
}
internal data class ReviewHistoryUiState(val complete: Boolean, val phase: String?, val updatedAt: String, val source: String)
internal fun reviewHistoryLabel(state: ReviewHistoryUiState): Int = when {
    state.complete -> R.string.review_history_complete
    state.phase == "stop" -> R.string.review_history_stopped
    else -> R.string.review_history_started
}

@Composable
internal fun ReviewHistoryRow(state: ReviewHistoryUiState) {
    Text(stringResource(R.string.review_history_row, state.updatedAt, stringResource(reviewHistoryLabel(state)), state.source))
}

@Composable
internal fun SavedWarningRow(state: SavedWarningUiState, onEvent: (SavedWarningUiEvent) -> Unit) {
    Text(stringResource(R.string.review_warning_timestamp, state.title, state.updatedAt))
    Button(onClick = { onEvent(SavedWarningUiEvent.OPEN) }, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = DesignTokens.touchMinimum)) {
        Text(stringResource(R.string.review_warning_open, state.title))
    }
    OutlinedButton(onClick = { onEvent(SavedWarningUiEvent.DELETE) }, enabled = !state.busy,
        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = DesignTokens.touchMinimum)) {
        Text(stringResource(R.string.review_warning_delete, state.title))
    }
}

@Composable
fun ReviewScreen(onPractice: () -> Unit, onWarning: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ReviewRepository(context) }
    val scope = rememberCoroutineScope()
    var record by remember { mutableStateOf(repository.load()) }
    var history by remember { mutableStateOf(repository.history()) }
    var reviewEvents by remember { mutableStateOf(repository.events()) }
    var consent by remember { mutableStateOf(repository.consent) }
    var warnings by remember { mutableStateOf(repository.items().filter { it.kind == "warning" }) }
    val resources = LocalResources.current
    val catalog = remember(resources) {
        runCatching { resources.openRawResource(R.raw.warning_catalog).bufferedReader().use { parseWarningCatalog(it.readText()) } }.getOrDefault(emptyList())
    }
    var storageStatus by remember { mutableStateOf(repository.storageStatus()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var warningToDelete by remember { mutableStateOf<ReviewItem?>(null) }
    var busy by remember { mutableStateOf(false) }
    var migrationMessage by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf("") }
    LaunchedEffect(repository) {
        if (repository.consent) {
            val migrated = withContext(Dispatchers.IO) { repository.migrateLegacy() }
            storageStatus = repository.storageStatus()
            if (storageStatus == ReviewStorageStatus.NORMAL) warnings = repository.items().filter { it.kind == "warning" }
            migrationMessage = if (!migrated && storageStatus == ReviewStorageStatus.NORMAL) resources.getString(R.string.review_migration_failed) else ""
        }
    }
    Text(stringResource(R.string.home_review), style = MaterialTheme.typography.headlineMedium)
    if (migrationMessage.isNotEmpty()) Text(migrationMessage)
    if (actionMessage.isNotEmpty()) Text(actionMessage)
    Text(stringResource(R.string.review_shared_device))
    ReviewRecoverySection(repository, storageStatus) {
        storageStatus = repository.storageStatus()
        if (storageStatus == ReviewStorageStatus.NORMAL) {
            consent = repository.consent
            record = repository.load()
            history = repository.history()
            reviewEvents = repository.events()
            warnings = repository.items().filter { it.kind == "warning" }
        }
    }
    Text(if (storageStatus == ReviewStorageStatus.WRITE_UNKNOWN) stringResource(R.string.review_storage_unknown) else stringResource(R.string.review_storage_status, stringResource(if (consent) R.string.backup_on else R.string.backup_off)))
    Text(stringResource(R.string.review_consent_removal_notice))
    if (storageStatus == ReviewStorageStatus.NORMAL) ReviewConsentControl(consent, !busy) { enabled ->
        busy = true
        scope.launch {
            val success = withContext(Dispatchers.IO) { repository.setConsent(enabled) }
            storageStatus = repository.storageStatus()
            if (success) { consent = enabled; record = repository.load(); history = repository.history(); reviewEvents = repository.events(); warnings = repository.items().filter { it.kind == "warning" } }
            busy = false
        }
    }
    val saved = if (storageStatus == ReviewStorageStatus.NORMAL) record else null
    Text(stringResource(R.string.review_wiper_title), style = MaterialTheme.typography.titleLarge)
    if (saved == null && storageStatus == ReviewStorageStatus.NORMAL) Text(stringResource(R.string.review_wiper_empty))
    else if (saved != null) {
        Text(stringResource(R.string.review_wiper_state, stringResource(if (saved.complete) R.string.review_history_complete else R.string.review_wiper_incomplete)), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.review_warning_timestamp, DateFormat.getDateTimeInstance().format(Date(saved.updatedAt)), saved.source))
        Button(onClick = onPractice, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.review_wiper_reopen)) }
        OutlinedButton(onClick = { confirmDelete = true }, enabled = !busy, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.review_wiper_delete)) }
    }
    if (storageStatus == ReviewStorageStatus.NORMAL && history.isNotEmpty()) {
        Text(stringResource(R.string.review_history_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.review_history_details))
        history.forEachIndexed { index, item ->
            val phase = reviewEvents.getOrNull(index)?.let(::attemptMarker)?.phase
            ReviewHistoryRow(ReviewHistoryUiState(item.complete, phase, DateFormat.getDateTimeInstance().format(Date(item.updatedAt)), item.source))
        }
    }
    if (storageStatus == ReviewStorageStatus.NORMAL) {
        Text(stringResource(R.string.warning_catalog_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.review_warning_not_detection))
        if (warnings.isEmpty()) Text(stringResource(R.string.review_warning_empty))
        warnings.forEach { item ->
            val title = catalog.firstOrNull { it.id == item.id }?.title ?: stringResource(R.string.review_missing_warning, item.id)
            SavedWarningRow(SavedWarningUiState(title, DateFormat.getDateTimeInstance().format(Date(item.updatedAt)), busy)) { event ->
                when (event) { SavedWarningUiEvent.OPEN -> onWarning(item.id); SavedWarningUiEvent.DELETE -> warningToDelete = item }
            }
        }
    }
    warningToDelete?.let { item ->
        val title = catalog.firstOrNull { it.id == item.id }?.title ?: item.id
        ReviewDeleteDialog(stringResource(R.string.review_warning_delete_title, title), stringResource(R.string.review_warning_delete_details)) { event ->
            if (event == ReviewDeleteEvent.CANCEL) warningToDelete = null else {
                warningToDelete = null; busy = true
                scope.launch {
                    val success = withContext(Dispatchers.IO) { repository.deleteWarning(item.id) }
                    storageStatus = repository.storageStatus()
                    if (success) warnings = repository.items().filter { it.kind == "warning" }
                    actionMessage = if (!success && storageStatus == ReviewStorageStatus.NORMAL) resources.getString(R.string.review_warning_delete_failed) else ""
                    busy = false
                }
            }
        }
    }
    if (confirmDelete) ReviewDeleteDialog(stringResource(R.string.review_wiper_delete_title), stringResource(R.string.review_wiper_delete_details)) { event ->
        if (event == ReviewDeleteEvent.CANCEL) confirmDelete = false else {
            confirmDelete = false; busy = true
            scope.launch {
                val success = withContext(Dispatchers.IO) { repository.delete() }
                storageStatus = repository.storageStatus()
                if (success) { record = null; history = emptyList(); reviewEvents = emptyList() }
                actionMessage = if (!success && storageStatus == ReviewStorageStatus.NORMAL) resources.getString(R.string.review_wiper_delete_failed) else ""
                busy = false
            }
        }
    }
    OutlinedButton(onClick = onBack, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.checklist_home)) }
}

/** Disabling consent deletes records, so a second explicit action is required. */
@Composable
internal fun ReviewConsentControl(enabled: Boolean, canChange: Boolean = true, onChange: (Boolean) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val consentLabel = stringResource(R.string.review_consent_label)
    Switch(checked = enabled, enabled = canChange, onCheckedChange = { if (it) onChange(true) else confirm = true },
        modifier = Modifier.semantics { contentDescription = consentLabel })
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.review_consent_off_title)) },
        text = { Text(stringResource(R.string.review_consent_off_details)) },
        confirmButton = { TextButton(onClick = { confirm = false; onChange(false) }) { Text(stringResource(R.string.review_consent_off_confirm)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
    )
}
