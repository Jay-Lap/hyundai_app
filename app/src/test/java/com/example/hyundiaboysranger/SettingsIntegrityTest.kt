package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test

class SettingsIntegrityTest {
    private val current = mapOf<String, Any>(
        "schema_version" to 1, "profile" to "senior", "large_title" to true,
        "prefer_wide_roads" to true, "prefer_highways" to false,
        "high_contrast" to true, "detailed_guidance" to false, "display_mode" to "NIGHT",
        "text_size_percent" to 175,
    )
    @Test fun validLegacyAndCurrentAreDistinct() {
        assertEquals(SettingsStorageKind.LEGACY, inspectSettings(emptyMap<String, Any>()).kind)
        assertEquals(SettingsStorageKind.LEGACY, inspectSettings(current - "schema_version").kind)
        assertEquals(SettingsStorageKind.CURRENT, inspectSettings(current).kind)
        assertEquals(SettingsStorageKind.CURRENT, inspectSettings(current - "text_size_percent").kind)
    }
    @Test fun wrongTypeRangeMissingAndFutureAreDistinct() {
        assertEquals(SettingsStorageStatus(SettingsStorageKind.WRONG_TYPE, "large_title"), inspectSettings(current + ("large_title" to "true")))
        assertEquals(SettingsStorageStatus(SettingsStorageKind.OUT_OF_RANGE, "text_size_percent"), inspectSettings(current + ("text_size_percent" to 201)))
        assertEquals(SettingsStorageStatus(SettingsStorageKind.MISSING, "display_mode"), inspectSettings(current - "display_mode"))
        assertEquals(SettingsStorageKind.FUTURE_VERSION, inspectSettings(current + ("schema_version" to 9) + ("large_title" to "unknown")).kind)
        assertEquals(SettingsStorageKind.OUT_OF_RANGE, inspectSettings(current + ("profile" to "other")).kind)
    }
}
