package com.example.hyundiaboysranger

import android.content.Context
import android.annotation.SuppressLint
import android.content.SharedPreferences

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
class SettingsRepository internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context, preferenceName: String = "user_settings") : this(
        context.applicationContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE))
    private val guard = PreferenceWriteGuards.forPreferences(preferences)
    fun status(): SettingsStorageStatus = synchronized(preferences) {
        if (guard.uncertain) SettingsStorageStatus(SettingsStorageKind.WRITE_UNKNOWN) else inspectSettings(preferences.all)
    }
    fun retryWrite(): Boolean = synchronized(preferences) { guard.retry() == WriteResult.SUCCESS }
    fun pendingWriteDescription(): String? = guard.pendingLabel

    fun load(): UserSettings {
        // Read legacy or damaged fields independently; never rewrite data while loading.
        val values = preferences.all
        val profile = values["profile"] as? String
        return UserSettings(
            profile = UserProfile.fromKey(profile),
            largeTitle = values["large_title"] as? Boolean ?: false,
            preferWideRoads = values["prefer_wide_roads"] as? Boolean ?: false,
            preferHighways = values["prefer_highways"] as? Boolean ?: false,
            highContrast = values["high_contrast"] as? Boolean ?: (profile == "senior"),
            detailedGuidance = values["detailed_guidance"] as? Boolean ?: (profile != "senior"),
            displayMode = DisplayMode.fromKey(values["display_mode"] as? String),
            textSizePercent = (values["text_size_percent"] as? Int)?.coerceIn(100, 200),
        )
    }

    fun loadIfReadable(): UserSettings? = synchronized(preferences) {
        if (status().writable) load() else null
    }

    fun observe(onChange: () -> Unit): () -> Unit {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val completed: () -> Unit = { handler.post { onChange() }; Unit }
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> if (!guard.uncertain) onChange() }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        guard.addListener(completed)
        return { guard.removeListener(completed); preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun write(editor: SharedPreferences.Editor, settings: UserSettings): SharedPreferences.Editor = editor
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
            .putInt("schema_version", 1)

    /** Valid legacy values are migrated together; invalid/future records remain untouched. */
    @SuppressLint("UseKtx")
    fun migrateLegacy(): Boolean = synchronized(preferences) {
        when (status().kind) {
            SettingsStorageKind.CURRENT -> true
            SettingsStorageKind.LEGACY -> {
                val settings = load()
                guard.execute("설정 형식 갱신") { androidCommit(write(preferences.edit(), settings)) } == WriteResult.SUCCESS
            }
            else -> false
        }
    }

    fun save(settings: UserSettings, description: String = "설정 저장"): Boolean = synchronized(preferences) {
        if (!status().writable || (settings.textSizePercent != null && settings.textSizePercent !in 100..200)) return@synchronized false
        guard.execute(description) { androidCommit(write(preferences.edit(), settings)) } == WriteResult.SUCCESS
    }

    /** Read and update under one lock so an acceptance cannot overwrite a newer setting. */
    fun enableDetailedGuidance(): Boolean = synchronized(preferences) {
        if (!status().writable) return@synchronized false
        val current = load()
        if (current.detailedGuidance) return@synchronized true
        save(current.copy(detailedGuidance = true), "상세 안내 켜기")
    }

    fun recoverySnapshot(): SettingsRecoverySnapshot? = synchronized(preferences) {
        val state = status()
        if (state.writable || state.kind == SettingsStorageKind.FUTURE_VERSION || state.kind == SettingsStorageKind.WRITE_UNKNOWN) null
        else SettingsRecoverySnapshot(preferences.all.mapValues { (_, value) -> if (value is Set<*>) value.toSet() else value })
    }

    @SuppressLint("UseKtx")
    fun resetDamagedAfterConfirmation(snapshot: SettingsRecoverySnapshot): Boolean = synchronized(preferences) {
        val state = status()
        if (state.writable || state.kind == SettingsStorageKind.FUTURE_VERSION || state.kind == SettingsStorageKind.WRITE_UNKNOWN || preferences.all != snapshot.values) return@synchronized false
        guard.execute("손상된 설정 초기화") { androidCommit(write(preferences.edit(), UserSettings())) } == WriteResult.SUCCESS
    }
}

class SettingsRecoverySnapshot internal constructor(val values: Map<String, *>)
