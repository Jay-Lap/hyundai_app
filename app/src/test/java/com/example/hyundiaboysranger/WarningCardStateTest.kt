package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class WarningCardStateTest {
    private val entry = WarningEntry("engine", "엔진", "설명", "확인")

    @Test fun addIsAllowedOnlyForUnsavedIdleReadableStorage() {
        val state = WarningCardState(entry, saved = false, busy = false, storageNormal = true)
        assertTrue(state.canAdd)
        assertFalse(state.copy(saved = true).canAdd)
        assertFalse(state.copy(busy = true).canAdd)
        assertFalse(state.copy(storageNormal = false).canAdd)
    }

    @Test fun homeDestinationsKeepTheExistingNavigationKeys() {
        assertEquals(listOf("checklist", "door", "route", "learn", "warnings", "warning_catalog", "review", "profile", "settings"), HomeDestination.entries.map { it.key })
    }
}
