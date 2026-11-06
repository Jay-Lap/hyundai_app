package com.example.hyundiaboysranger

import ai.pleos.playground.navi.constants.*
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** No synthetic routes: only service callbacks confirm requested changes. */
class NavigationSession(private val context: Context) {
    var status by mutableStateOf("내비게이션에 연결하지 않았어요."); private set
    var connected by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var destination by mutableStateOf<DestinationInfo?>(null); private set
    var routeState by mutableStateOf<RouteStateInfo?>(null); private set
    var bookmarks by mutableStateOf<List<BookmarkInfo>>(emptyList()); private set
    var updatedAt by mutableStateOf(0L); private set
    private var helper: NaviHelper? = null
    private var listener: NaviHelperEventListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private var generation = 0
    private var request = 0
    private var expectedOption: RouteOption? = null
    private var pending = ""
    private val waiting = mutableSetOf<String>()

    fun activeRoute(now: Long): Boolean = connected && now - updatedAt in 0..15_000 &&
        routeState?.naviMode in listOf(NaviMode.ROUTE_GUIDING, NaviMode.SIMULATION_GUIDING)

    fun connect() {
        disconnect()
        val token = generation
        busy = true; status = "내비게이션 연결 중…"
        fun receive(action: () -> Unit) { handler.post { if (token == generation) action() } }
        val events = object : NaviHelperEventListener {
            override fun onDestinationInfo(destinationInfo: DestinationInfo) = receive {
                if (waiting.remove("destination")) { destination = destinationInfo; finishRefresh() }
            }
            override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) = receive {
                if (waiting.remove("state")) {
                    routeState = routeStateInfo
                    if (routeStateInfo.naviMode in listOf(NaviMode.ROUTE_GUIDING, NaviMode.SIMULATION_GUIDING)) {
                        waiting.add("destination")
                        try { helper?.getDestinationInfo() } catch (_: Exception) { fail("목적지 조회에 실패했어요.") }
                    }
                    finishRefresh()
                }
            }
            override fun onBookmarkInfo(bookmarkInfoList: List<BookmarkInfo>) = receive {
                if (waiting.remove("bookmarks")) { bookmarks = bookmarkInfoList.take(20); complete("즐겨찾기를 조회했어요.", invalidateRoute = false) }
            }
            override fun onRouteOptionChanged(routeOption: RouteOption) = receive {
                if (pending == "option" && routeOption == expectedOption) complete("경로 옵션 변경을 확인했어요. 정보를 다시 조회해 주세요.")
            }
            override fun onRoutingCompleted() = receive {
                if (pending == "route") complete("경로 탐색 완료 응답을 받았어요. 기본 내비게이션에서 경로를 확인해 주세요.")
            }
            override fun onRouteCancelled() = receive {
                destination = null; routeState = null
                if (pending == "cancel") complete("경로 안내 종료를 확인했어요.")
            }
            override fun onDestinationChanged() = receive {
                destination = null; routeState = null; updatedAt = 0
            }
            override fun onError(errorCode: NaviErrorCode) = receive {
                fail("내비게이션 요청 오류: ${errorCode.name}. 기본 내비게이션 상태를 확인해 주세요.")
            }
        }
        listener = events
        try {
            val service = NaviHelper(context.applicationContext)
            helper = service
            armTimeout()
            service.initialize(onConnected = { receive {
                try {
                    service.addListener(events); connected = true
                    busy = false; request++
                    status = "연결됐어요. 정보 조회를 눌러 주세요."
                } catch (_: Exception) { fail("내비게이션 이벤트 연결에 실패했어요.") }
            } }, onFailed = { receive { fail("내비게이션 서비스에 연결하지 못했어요.") } })
        } catch (_: Exception) { fail("내비게이션 SDK를 시작하지 못했어요.") }
    }

    fun refresh() {
        if (!connected || busy) return
        destination = null; routeState = null; updatedAt = 0
        waiting.add("state")
        send("refresh", "목적지와 경로를 조회하고 있어요.") {
            it.getRouteStateInfo()
        }
    }
    fun loadBookmarks() {
        if (!connected || busy) return
        bookmarks = emptyList(); waiting.add("bookmarks")
        send("bookmarks", "즐겨찾기를 조회하고 있어요.") { it.getBookmarkInfo() }
    }
    private fun finishRefresh() {
        if (waiting.isEmpty() && pending == "refresh") {
            updatedAt = SystemClock.elapsedRealtime()
            complete("내비게이션 정보를 조회했어요. 마지막 조회값이며 현재와 다를 수 있어요.", invalidateRoute = false)
        }
    }
    fun changeOption(option: RouteOption) {
        if (!activeRoute(SystemClock.elapsedRealtime())) return
        expectedOption = option
        send("option", "경로 옵션 변경을 요청했어요. 응답을 기다립니다.") { it.changeRouteOption(option) }
    }
    fun requestBookmark(bookmark: BookmarkInfo, option: RouteOption) {
        val subId = bookmark.poiSubId
        if (subId == null || !bookmark.latitude.isFinite() || !bookmark.longitude.isFinite() ||
            bookmark.latitude !in -90.0..90.0 || bookmark.longitude !in -180.0..180.0 || bookmark.poiId.isBlank()) {
            status = "목적지 정보가 올바르지 않아 요청하지 않았어요."; return
        }
        send("route", "목적지 경로를 요청했어요. 완료 응답을 기다립니다.") {
            it.requestRoute(RouteInfo(latitude = bookmark.latitude, longitude = bookmark.longitude,
                displayLatitude = bookmark.displayLatitude, displayLongitude = bookmark.displayLongitude,
                poiName = bookmark.name, poiId = bookmark.poiId, address = bookmark.address,
                poiSubId = subId, poiProvider = bookmark.poiProvider, routeOption = option))
        }
    }
    fun cancelRoute() {
        if (activeRoute(SystemClock.elapsedRealtime())) send("cancel", "경로 안내 종료 응답을 기다립니다.") { it.cancelRoute() }
    }
    private fun send(kind: String, message: String, operation: (NaviHelper) -> Unit) {
        val service = helper ?: return
        if (!connected || busy) return
        pending = kind; busy = true; status = message; armTimeout()
        try { operation(service) } catch (_: Exception) { fail("요청을 보내지 못했어요. 권한과 내비게이션 상태를 확인해 주세요.") }
    }
    private fun armTimeout() {
        val id = ++request
        val token = generation
        handler.postDelayed({ if (token == generation && id == request && busy) {
            fail("응답을 확인하지 못했어요. 작업이 처리됐을 수도 있으니 기본 내비게이션을 확인한 뒤 다시 연결해 주세요.")
        } }, 15_000)
    }
    private fun complete(message: String, invalidateRoute: Boolean = true) {
        busy = false; pending = ""; request++; expectedOption = null; status = message
        if (invalidateRoute) { updatedAt = 0; routeState = null }
    }
    private fun fail(message: String) { disconnect(); status = message }
    fun disconnect() {
        generation++; request++; handler.removeCallbacksAndMessages(null)
        val service = helper; helper = null
        try { listener?.let { service?.removeListener(it) } } catch (_: Exception) { }
        try { service?.release() } catch (_: Exception) { }
        listener = null; connected = false; busy = false; pending = ""; expectedOption = null
        waiting.clear(); destination = null; routeState = null; bookmarks = emptyList(); updatedAt = 0
    }
}
