package com.example.hyundiaboysranger

internal enum class NavigationObservation { ROUTING_COMPLETED, ROUTE_CANCELLED }

/** SDK events have no request ID, so neither event completes a pending request. */
internal fun uncorrelatedNavigationEvent(event: NavigationObservation): String = when (event) {
    NavigationObservation.ROUTING_COMPLETED -> "경로 탐색 완료 이벤트를 받았어요. 이 요청의 결과인지 확인할 수 없어요. 기본 내비게이션에서 확인해 주세요."
    NavigationObservation.ROUTE_CANCELLED -> "경로 종료 이벤트를 받았어요. 이 요청의 결과인지 확인할 수 없어요. 기본 내비게이션에서 확인해 주세요."
}
