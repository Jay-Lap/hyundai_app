package com.example.hyundiaboysranger

/** Incomplete or future records must be preserved, not replaced with default values. */
internal fun invalidReviewHeader(values: Map<String, *>, supportedVersion: Int): Boolean {
    val version = values["schema_version"]
    if (version != null && (version !is Int || version !in 1..supportedVersion)) return true
    if (values["consent"] != null && values["consent"] !is Boolean) return true
    if (values["source"] != null && values["source"] !is String) return true
    val updated = values["updated"]
    val complete = values["complete"]
    if (updated == null && complete == null) return false
    return updated !is Long || updated <= 0L || complete !is Boolean
}
