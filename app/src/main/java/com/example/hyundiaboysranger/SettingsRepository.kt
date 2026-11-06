package com.example.hyundiaboysranger

import android.content.Context

enum class UserProfile(val key: String, val label: String) {
    SENIOR("senior", "고령자"),
    BEGINNER("beginner", "초보자"),
    CUSTOM("custom", "직접 설정");

    companion object {
        fun fromKey(key: String?): UserProfile = entries.firstOrNull { it.key == key } ?: CUSTOM
    }
}

enum class DisplayMode(val label: String) {
    SYSTEM("기기 설정"), DAY("주간"), NIGHT("야간");
    companion object {
        fun fromKey(key: String?): DisplayMode = entries.firstOrNull { it.name == key } ?: SYSTEM
    }
}

data class UserSettings(
    val profile: UserProfile = UserProfile.CUSTOM,
    val largeTitle: Boolean = false,
    val preferWideRoads: Boolean = false,
    val preferHighways: Boolean = false,
    val highContrast: Boolean = false,
    val detailedGuidance: Boolean = true,
    val displayMode: DisplayMode = DisplayMode.SYSTEM,
    val textSizePercent: Int? = null,
) {
    val effectiveTextSizePercent: Int
        get() = textSizePercent?.coerceIn(100, 200) ?: if (largeTitle) 140 else 100

    fun withTextSize(percent: Int): UserSettings = copy(textSizePercent = percent.coerceIn(100, 200))

    fun withProfile(selected: UserProfile): UserSettings = when (selected) {
        UserProfile.SENIOR -> copy(profile = selected, largeTitle = true, highContrast = true, detailedGuidance = false)
        UserProfile.BEGINNER -> copy(profile = selected, largeTitle = false, highContrast = false, detailedGuidance = true)
        UserProfile.CUSTOM -> copy(profile = selected)
    }
}

/** Keeps storage separate from the UI so the settings implementation can be replaced later. */
class SettingsRepository(context: Context, preferenceName: String = "user_settings") {
    private val preferences = context.applicationContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)

    fun load(): UserSettings = UserSettings(
        profile = UserProfile.fromKey(preferences.getString("profile", null)),
        largeTitle = preferences.getBoolean("large_title", false),
        preferWideRoads = preferences.getBoolean("prefer_wide_roads", false),
        preferHighways = preferences.getBoolean("prefer_highways", false),
        highContrast = preferences.getBoolean("high_contrast", preferences.getString("profile", null) == "senior"),
        detailedGuidance = preferences.getBoolean("detailed_guidance", preferences.getString("profile", null) != "senior"),
        displayMode = DisplayMode.fromKey(preferences.getString("display_mode", null)),
        textSizePercent = if (preferences.contains("text_size_percent"))
            preferences.getInt("text_size_percent", 100).coerceIn(100, 200) else null,
    )

    fun observe(onChange: (UserSettings) -> Unit): () -> Unit {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> onChange(load()) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun save(settings: UserSettings) {
        preferences.edit()
            .putString("profile", settings.profile.key)
            .putBoolean("large_title", settings.largeTitle)
            .putBoolean("prefer_wide_roads", settings.preferWideRoads)
            .putBoolean("prefer_highways", settings.preferHighways)
            .putBoolean("high_contrast", settings.highContrast)
            .putBoolean("detailed_guidance", settings.detailedGuidance)
            .putString("display_mode", settings.displayMode.name)
            .also { editor ->
                if (settings.textSizePercent == null) editor.remove("text_size_percent")
                else editor.putInt("text_size_percent", settings.effectiveTextSizePercent)
            }
            .apply()
    }
}
