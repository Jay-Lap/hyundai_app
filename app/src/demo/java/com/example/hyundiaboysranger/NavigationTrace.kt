package com.example.hyundiaboysranger

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class NavigationTrace {
    var entries by mutableStateOf<List<String>>(emptyList()); private set
    fun record(event: NavigationTraceEvent) {
        val label = when (event) {
            NavigationTraceEvent.ROUTE_STATE -> "경로 상태 응답"
            NavigationTraceEvent.OPTION_CALLBACK -> "옵션 변경 콜백"
            NavigationTraceEvent.ERROR -> "오류 콜백"
            NavigationTraceEvent.CONNECTED -> "서비스 연결 콜백"
            NavigationTraceEvent.OPTION_REQUEST -> "옵션 요청"
            NavigationTraceEvent.REQUEST -> "요청"
            NavigationTraceEvent.TIMEOUT -> "시간 초과, 처리 여부 미확인"
        }
        entries = (entries + label).takeLast(12)
    }
}
