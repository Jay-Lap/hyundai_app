package com.example.hyundiaboysranger

/** All fields must come from the provider; absent road data must never be invented. */
data class ComparableRoute(val id: String, val minutes: Int, val turns: Int, val narrowSections: Int, val highwayPercent: Int)
data class RoutePreferences(val avoidNarrow: Boolean, val preferHighway: Boolean)
data class ScoredRoute(val route: ComparableRoute, val score: Long)

fun rankRoutes(routes: List<ComparableRoute>, preferences: RoutePreferences): List<ScoredRoute> = routes
    .filter { it.minutes > 0 && it.turns >= 0 && it.narrowSections >= 0 && it.highwayPercent in 0..100 }
    .map { route ->
        // A provisional comparison only, not a validated safety metric.
        val score = route.minutes.toLong() + route.turns.toLong() * 2L +
            route.narrowSections.toLong() * (if (preferences.avoidNarrow) 12L else 4L) -
            (if (preferences.preferHighway) route.highwayPercent / 10L else 0L)
        ScoredRoute(route, score)
    }.sortedWith(compareBy<ScoredRoute> { it.score }.thenBy { it.route.id })

interface RouteProvider { fun candidates(): List<ComparableRoute> }
/** NaviHelper's current integration does not expose verified candidate attributes. */
class SdkRouteProvider : RouteProvider { override fun candidates(): List<ComparableRoute> = emptyList() }
