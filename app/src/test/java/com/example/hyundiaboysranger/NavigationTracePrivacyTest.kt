package com.example.hyundiaboysranger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NavigationTracePrivacyTest {
    @Test fun traceAcceptsOnlyEventNamesAndKeepsBoundedHistory() {
        val trace = NavigationTrace()
        repeat(20) { trace.record(NavigationTraceEvent.REQUEST) }
        assertEquals(12, trace.entries.size)
        assertEquals(setOf("요청"), trace.entries.toSet())
        assertFalse(trace.entries.any { it.contains("위치") || it.contains("목적지") })
    }
}
