package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class RouteComparisonTest {
    @Test fun largeValuesCannotOverflowIntoAFalseRecommendation() {
        val ordinary = ComparableRoute("A", 20, 3, 0, 0)
        val extreme = ComparableRoute("B", Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, 0)
        val ranked = rankRoutes(listOf(extreme, ordinary), RoutePreferences(true, false))
        assertEquals("A", ranked.first().route.id)
        assertTrue(ranked.last().score > Int.MAX_VALUE.toLong())
    }
    @Test fun preferencesChangeRankingOnlyWithSuppliedAttributes() {
        val routes = listOf(ComparableRoute("A", 20, 5, 2, 0), ComparableRoute("B", 35, 3, 0, 70))
        assertEquals("A", rankRoutes(routes, RoutePreferences(false, false)).first().route.id)
        assertEquals("B", rankRoutes(routes, RoutePreferences(true, false)).first().route.id)
        assertEquals("B", rankRoutes(routes, RoutePreferences(false, true)).first().route.id)
        assertEquals("B", rankRoutes(routes, RoutePreferences(true, true)).first().route.id)
    }
    @Test fun invalidOrMissingCandidatesNeverProduceARecommendation() {
        assertTrue(rankRoutes(emptyList(), RoutePreferences(true, true)).isEmpty())
        assertTrue(rankRoutes(listOf(ComparableRoute("x", 0, 1, 0, 0)), RoutePreferences(false, false)).isEmpty())
        assertTrue(SdkRouteProvider().candidates().isEmpty())
    }
}
