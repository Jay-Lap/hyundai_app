package com.example.hyundiaboysranger

/** Versioned, bounded, all-or-nothing interchange; no vehicle or review data included. */
object SettingsBackup {
    private val keys = setOf("format", "profile", "largeTitle", "preferWideRoads", "preferHighways", "highContrast", "detailedGuidance", "displayMode", "textSizePercent")
    fun encode(settings: UserSettings): String = listOf(
        "format=pleos-assist-settings-v1", "profile=${settings.profile.name}", "largeTitle=${settings.largeTitle}",
        "preferWideRoads=${settings.preferWideRoads}", "preferHighways=${settings.preferHighways}",
        "highContrast=${settings.highContrast}", "detailedGuidance=${settings.detailedGuidance}",
        "displayMode=${settings.displayMode.name}", "textSizePercent=${settings.textSizePercent ?: "default"}",
    ).joinToString("\n", postfix = "\n")

    fun decode(text: String): UserSettings? = try {
        require(text.length <= 4096)
        val entries = text.lineSequence().map(String::trim).filter(String::isNotEmpty).map {
            val pair = it.split('=', limit = 2); require(pair.size == 2); pair[0] to pair[1]
        }.toList()
        require(entries.size == keys.size && entries.map { it.first }.toSet() == keys)
        val values = entries.toMap()
        require(values.getValue("format") == "pleos-assist-settings-v1")
        fun flag(key: String) = values.getValue(key).toBooleanStrict()
        val size = values.getValue("textSizePercent").let { if (it == "default") null else it.toInt().also { n -> require(n in 100..200) } }
        UserSettings(UserProfile.valueOf(values.getValue("profile")), flag("largeTitle"), flag("preferWideRoads"),
            flag("preferHighways"), flag("highContrast"), flag("detailedGuidance"),
            DisplayMode.valueOf(values.getValue("displayMode")), size)
    } catch (_: IllegalArgumentException) { null }
}
