package com.example.hyundiaboysranger

import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.BookmarkInfo
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.hyundiaboysranger.ui.theme.DesignTokens
import kotlinx.coroutines.delay

@Composable
fun RouteOption.label(): String = stringResource(when (this) {
    RouteOption.RECOMMENDED -> R.string.nav_option_recommended
    RouteOption.HIGHWAY -> R.string.nav_option_highway
    RouteOption.COMFORTABLE -> R.string.nav_option_comfortable
    RouteOption.FREE -> R.string.nav_option_free
    RouteOption.SHORT_DISTANCE -> R.string.nav_option_distance
    RouteOption.SHORT_TIME -> R.string.nav_option_time
    RouteOption.SCHOOL_ZONE -> R.string.nav_option_school
})

internal data class NavigationConnectionState(val connected: Boolean, val busy: Boolean)
internal enum class NavigationConnectionEvent { CONNECT, REFRESH }

@Composable
internal fun NavigationConnectionControls(state: NavigationConnectionState, onEvent: (NavigationConnectionEvent) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.gapSmall), verticalArrangement = Arrangement.spacedBy(DesignTokens.gapSmall)) {
        OutlinedButton(onClick = { onEvent(NavigationConnectionEvent.CONNECT) }, enabled = !state.busy, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.nav_connect)) }
        Button(onClick = { onEvent(NavigationConnectionEvent.REFRESH) }, enabled = state.connected && !state.busy, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.nav_refresh)) }
    }
}

@Composable
fun NavigationScreen(preferHighways: Boolean) {
    val context = LocalContext.current
    val session = remember { NavigationSession(context) }
    var option by remember(preferHighways) { mutableStateOf(if (preferHighways) RouteOption.HIGHWAY else RouteOption.RECOMMENDED) }
    var selected by remember { mutableStateOf<BookmarkInfo?>(null) }
    var confirmCancel by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { now = SystemClock.elapsedRealtime(); delay(500) } }
    DisposableEffect(session) { session.connect(); onDispose { session.disconnect() } }
    Text(stringResource(R.string.nav_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.nav_description))
    Text(session.status)
    session.optionEvidence?.let { evidence ->
        Text(stringResource(R.string.nav_evidence, evidence.before, evidence.requested, evidence.callback ?: stringResource(R.string.nav_not_received), evidence.after ?: stringResource(R.string.nav_not_confirmed)))
        Text(stringResource(R.string.nav_evidence_uncorrelated))
    }
    NavigationDebugDetails(session)
    NavigationConnectionControls(NavigationConnectionState(session.connected, session.busy)) { event ->
        when (event) { NavigationConnectionEvent.CONNECT -> session.connect(); NavigationConnectionEvent.REFRESH -> session.refresh() }
    }
    session.destination?.let { info ->
        val unnamed = stringResource(R.string.nav_unnamed)
        Text(stringResource(R.string.nav_destination, info.name.ifBlank { unnamed }))
        Text(info.address)
    }
    session.routeState?.let { state -> Text(stringResource(R.string.nav_reported_option, state.routeOption.label())) }
    if (session.updatedAt > 0 && now - session.updatedAt > 15_000) Text(stringResource(R.string.nav_stale))
    Text(stringResource(R.string.nav_option_title))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(RouteOption.RECOMMENDED, RouteOption.HIGHWAY, RouteOption.COMFORTABLE, RouteOption.FREE).forEach { candidate ->
            FilterChip(selected = option == candidate, onClick = { option = candidate }, label = { Text(candidate.label()) }, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum))
        }
    }
    Text(stringResource(R.string.nav_option_disclaimer))
    Button(onClick = { session.changeOption(option) }, enabled = !session.busy && session.activeRoute(SystemClock.elapsedRealtime()), modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.nav_apply_option)) }
    Text(stringResource(R.string.nav_bookmarks_title), style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = session::loadBookmarks, enabled = session.connected && !session.busy, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.nav_bookmarks_query)) }
    if (session.bookmarks.isEmpty()) Text(stringResource(R.string.nav_bookmarks_empty))
    session.bookmarks.forEach { bookmark ->
        val bookmarkName = bookmark.name.ifBlank { bookmark.address }
        val accessibleName = stringResource(R.string.nav_bookmark_accessibility, bookmarkName, bookmark.address)
        OutlinedButton(onClick = { selected = bookmark }, enabled = session.connected && !session.busy, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum).semantics { contentDescription = accessibleName }) {
            Text(bookmark.name.ifBlank { bookmark.address })
        }
    }
    OutlinedButton(onClick = { confirmCancel = true }, enabled = !session.busy && session.activeRoute(SystemClock.elapsedRealtime()), modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(stringResource(R.string.nav_stop)) }
    selected?.let { bookmark ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(stringResource(R.string.nav_route_confirm_title)) },
            text = { Text(stringResource(R.string.nav_route_confirm_details, bookmark.name, bookmark.address, option.label())) },
            confirmButton = { TextButton(onClick = { session.requestBookmark(bookmark, option); selected = null }) { Text(stringResource(R.string.nav_request)) } },
            dismissButton = { TextButton(onClick = { selected = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false }, title = { Text(stringResource(R.string.nav_stop_confirm)) },
        confirmButton = { TextButton(onClick = { session.cancelRoute(); confirmCancel = false }) { Text(stringResource(R.string.nav_stop_action)) } },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.action_cancel)) } })
}
