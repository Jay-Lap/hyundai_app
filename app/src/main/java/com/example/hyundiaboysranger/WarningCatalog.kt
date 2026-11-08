package com.example.hyundiaboysranger

data class WarningEntry(val id: String, val title: String, val description: String, val action: String)

/** Ignore malformed or duplicate rows; this catalogue does not determine vehicle severity. */
fun parseWarningCatalog(text: String): List<WarningEntry> {
    val seen = mutableSetOf<String>()
    return text.lineSequence().mapNotNull { line ->
        val parts = line.split('\t')
        if (parts.size != 4 || parts.any { it.isBlank() } || !parts[0].matches(Regex("[a-z]+")) || !seen.add(parts[0])) null
        else WarningEntry(parts[0], parts[1], parts[2], parts[3])
    }.toList()
}
