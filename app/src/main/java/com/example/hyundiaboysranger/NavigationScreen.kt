package com.example.hyundiaboysranger

import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.BookmarkInfo
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

fun RouteOption.label(): String = when (this) {
    RouteOption.RECOMMENDED -> "추천"
    RouteOption.HIGHWAY -> "고속도로 우선"
    RouteOption.COMFORTABLE -> "편안한 경로"
    RouteOption.FREE -> "무료 도로 우선"
    RouteOption.SHORT_DISTANCE -> "최단 거리"
    RouteOption.SHORT_TIME -> "최단 시간"
    RouteOption.SCHOOL_ZONE -> "스쿨존 고려"
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
    Text("기본 내비게이션 연동", style = MaterialTheme.typography.titleLarge)
    Text("기본 내비게이션 SDK 응답을 표시해요.")
    Text(session.status)
    session.optionEvidence?.let { evidence ->
        Text("옵션 관찰 · 요청 전 ${evidence.before} / 요청 ${evidence.requested} / 콜백 ${evidence.callback ?: "미수신"} / 재조회 ${evidence.after ?: "미확인"}")
        Text("요청 식별자가 없어 콜백과 재조회가 이 요청의 결과인지는 확정할 수 없어요.")
    }
    NavigationDebugDetails(session)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = session::connect, enabled = !session.busy) { Text("다시 연결") }
        Button(onClick = session::refresh, enabled = session.connected && !session.busy) { Text("정보 조회") }
    }
    session.destination?.let { info ->
        Text("마지막 조회 목적지: ${info.name.ifBlank { "이름 없음" }}")
        Text(info.address)
    }
    session.routeState?.let { state -> Text("조회된 경로 옵션: ${state.routeOption.label()}") }
    if (session.updatedAt > 0 && now - session.updatedAt > 15_000) Text("조회값이 오래됐어요. 경로를 변경하려면 다시 조회해 주세요.")
    Text("요청할 경로 옵션")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(RouteOption.RECOMMENDED, RouteOption.HIGHWAY, RouteOption.COMFORTABLE, RouteOption.FREE).forEach { candidate ->
            FilterChip(selected = option == candidate, onClick = { option = candidate }, label = { Text(candidate.label()) })
        }
    }
    Text("‘편안한 경로’는 내비게이션의 경로 옵션이에요. 도로 폭에 따른 큰길 우선이나 안전을 보장하지 않아요.")
    Button(onClick = { session.changeOption(option) }, enabled = !session.busy && session.activeRoute(SystemClock.elapsedRealtime())) { Text("현재 경로에 옵션 적용") }
    Text("내비게이션 즐겨찾기", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = session::loadBookmarks, enabled = session.connected && !session.busy) { Text("즐겨찾기 조회") }
    if (session.bookmarks.isEmpty()) Text("조회된 즐겨찾기가 없어요. 기본 내비게이션에서 등록한 뒤 조회해 주세요.")
    session.bookmarks.forEach { bookmark ->
        OutlinedButton(onClick = { selected = bookmark }, enabled = session.connected && !session.busy) {
            Text(bookmark.name.ifBlank { bookmark.address })
        }
    }
    OutlinedButton(onClick = { confirmCancel = true }, enabled = !session.busy && session.activeRoute(SystemClock.elapsedRealtime())) { Text("경로 안내 종료") }
    selected?.let { bookmark ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("이 목적지로 경로를 요청할까요?") },
            text = { Text("${bookmark.name}\n${bookmark.address}\n${option.label()} · 기존 경로가 변경될 수 있어요.") },
            confirmButton = { TextButton(onClick = { session.requestBookmark(bookmark, option); selected = null }) { Text("요청") } },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("취소") } })
    }
    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false }, title = { Text("현재 경로 안내를 종료할까요?") },
        confirmButton = { TextButton(onClick = { session.cancelRoute(); confirmCancel = false }) { Text("종료") } },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("취소") } })
}
