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

data class UserSettings(
    val profile: UserProfile = UserProfile.CUSTOM,
    val largeTitle: Boolean = false,
) {
    fun withProfile(selected: UserProfile): UserSettings = when (selected) {
        UserProfile.SENIOR -> copy(profile = selected, largeTitle = true)
        UserProfile.BEGINNER -> copy(profile = selected, largeTitle = false)
        UserProfile.CUSTOM -> copy(profile = selected)
    }
}

/** Keeps storage separate from the UI so the settings implementation can be replaced later. */
class SettingsRepository(context: Context, preferenceName: String = "user_settings") {
    private val preferences = context.applicationContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)

    fun load(): UserSettings = UserSettings(
        profile = UserProfile.fromKey(preferences.getString("profile", null)),
        largeTitle = preferences.getBoolean("large_title", false),
    )

    fun save(settings: UserSettings) {
        preferences.edit()
            .putString("profile", settings.profile.key)
            .putBoolean("large_title", settings.largeTitle)
            .apply()
    }
}
