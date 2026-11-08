package com.example.hyundiaboysranger

enum class SettingsStorageKind { LEGACY, CURRENT, WRONG_TYPE, OUT_OF_RANGE, MISSING, FUTURE_VERSION, WRITE_UNKNOWN }
data class SettingsStorageStatus(val kind: SettingsStorageKind, val field: String? = null) {
    val writable: Boolean get() = kind == SettingsStorageKind.LEGACY || kind == SettingsStorageKind.CURRENT
}

/** Missing legacy keys are allowed because older app versions did not store them. */
internal fun inspectSettings(values: Map<String, *>, currentVersion: Int = 1): SettingsStorageStatus {
    val schema = values["schema_version"]
    if (schema != null && schema !is Int) return SettingsStorageStatus(SettingsStorageKind.WRONG_TYPE, "schema_version")
    if (schema is Int && schema > currentVersion) return SettingsStorageStatus(SettingsStorageKind.FUTURE_VERSION, "schema_version")
    if (schema is Int && schema != currentVersion) return SettingsStorageStatus(SettingsStorageKind.OUT_OF_RANGE, "schema_version")
    val fields = listOf("profile", "large_title", "prefer_wide_roads", "prefer_highways", "high_contrast", "detailed_guidance", "display_mode")
    if (schema != null) fields.firstOrNull { !values.containsKey(it) }?.let { return SettingsStorageStatus(SettingsStorageKind.MISSING, it) }
    for (field in fields) {
        val value = values[field] ?: continue
        val expectedString = field == "profile" || field == "display_mode"
        if ((expectedString && value !is String) || (!expectedString && value !is Boolean))
            return SettingsStorageStatus(SettingsStorageKind.WRONG_TYPE, field)
        if (field == "profile" && value !in setOf("senior", "beginner", "custom"))
            return SettingsStorageStatus(SettingsStorageKind.OUT_OF_RANGE, field)
        if (field == "display_mode" && value !in setOf("SYSTEM", "DAY", "NIGHT"))
            return SettingsStorageStatus(SettingsStorageKind.OUT_OF_RANGE, field)
    }
    val size = values["text_size_percent"]
    if (size != null && size !is Int) return SettingsStorageStatus(SettingsStorageKind.WRONG_TYPE, "text_size_percent")
    if (size is Int && size !in 100..200) return SettingsStorageStatus(SettingsStorageKind.OUT_OF_RANGE, "text_size_percent")
    return SettingsStorageStatus(if (schema == null) SettingsStorageKind.LEGACY else SettingsStorageKind.CURRENT)
}
