package com.example.hyundiaboysranger

import org.json.JSONArray
import java.util.UUID
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hyundiaboysranger.ui.theme.DesignTokens

internal data class ReviewOverview(val items: List<ReviewItem>, val events: List<ReviewEvent>)
internal data class AttemptMarker(val id: String, val phase: String)

private fun rawAttemptId(event: ReviewEvent): String? = try {
    JSONArray("[${event.original}]").optJSONObject(0)?.opt("attempt_id") as? String
} catch (_: Exception) { null }

internal fun attemptMarker(event: ReviewEvent): AttemptMarker? {
    return try {
    val objectValue = JSONArray("[${event.original}]").optJSONObject(0) ?: return null
    if (objectValue.opt("attempt_format") != "explicit_stop_v1") return null
    val id = objectValue.opt("attempt_id") as? String ?: return null
    val phase = objectValue.opt("attempt_phase") as? String ?: return null
    if (UUID.fromString(id).toString() != id || phase !in setOf("start", "complete", "stop")) return null
    if ((phase == "complete") != (event.state == "complete")) return null
    AttemptMarker(id, phase)
    } catch (_: Exception) { null }
}

/** Two latest attempts must each contain exactly a verified start followed by a user stop. */
internal fun hasRepeatedExplicitStops(events: List<ReviewEvent>): Boolean {
    if (events.size < 4) return false
    val recent = events.take(4)
    if (recent.any { it.kind != "wiper" || it.id != "front_wiper" }) return false
    if (recent.zipWithNext().any { (newer, older) -> newer.updatedAt < older.updatedAt }) return false
    val markers = recent.map { attemptMarker(it) ?: return false }
    if (markers.map { it.phase } != listOf("stop", "start", "stop", "start")) return false
    val first = markers[0].id; val second = markers[2].id
    if (first != markers[1].id || second != markers[3].id || first == second) return false
    val allIds = events.mapNotNull(::rawAttemptId)
    return allIds.count { it == first } == 2 && allIds.count { it == second } == 2
}

internal fun todayItems(overview: ReviewOverview?, profile: UserProfile): List<ReviewItem> =
    overview?.items.orEmpty().sortedWith(compareByDescending<ReviewItem> { it.updatedAt }.thenBy { it.kind }.thenBy { it.id })
        .take(if (profile == UserProfile.SENIOR) 1 else 3)

internal data class TodayReviewItemState(val item: ReviewItem, val title: String, val senior: Boolean)

@Composable
internal fun TodayReviewItem(state: TodayReviewItemState, onOpen: (ReviewItem) -> Unit) {
    Button(onClick = { onOpen(state.item) }, modifier = Modifier.fillMaxWidth().sizeIn(minHeight = if (state.senior) 88.dp else DesignTokens.touchMinimum)) {
        Text(state.title, style = if (state.senior) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun TodayReviewSection(overview: ReviewOverview?, profile: UserProfile, onOpen: (ReviewItem) -> Unit) {
    val resources = LocalResources.current
    val catalog = remember(resources) {
        runCatching { resources.openRawResource(R.raw.warning_catalog).bufferedReader().use { parseWarningCatalog(it.readText()) } }.getOrDefault(emptyList())
    }
    Text(stringResource(R.string.today_review_title), style = MaterialTheme.typography.titleLarge)
    val selected = todayItems(overview, profile)
    if (overview == null) Text(stringResource(R.string.today_review_unavailable))
    else if (selected.isEmpty()) Text(stringResource(R.string.today_review_empty))
    selected.forEach { item ->
        val title = if (item.kind == "wiper") stringResource(R.string.today_wiper, stringResource(if (item.state == "complete") R.string.review_history_complete else R.string.today_wiper_pending))
        else stringResource(R.string.today_warning, catalog.firstOrNull { it.id == item.id }?.title ?: stringResource(R.string.today_catalog_item, item.id))
        TodayReviewItem(TodayReviewItemState(item, title, profile == UserProfile.SENIOR), onOpen)
    }
}
