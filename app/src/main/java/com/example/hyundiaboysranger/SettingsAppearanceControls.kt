package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.hyundiaboysranger.ui.theme.DesignTokens

internal sealed interface AppearanceEvent {
    data class Contrast(val enabled: Boolean) : AppearanceEvent
    data class Mode(val value: DisplayMode) : AppearanceEvent
    data class Detailed(val enabled: Boolean) : AppearanceEvent
}

internal fun reduceAppearance(settings: UserSettings, event: AppearanceEvent): UserSettings = when (event) {
    is AppearanceEvent.Contrast -> settings.copy(profile = UserProfile.CUSTOM, highContrast = event.enabled)
    is AppearanceEvent.Mode -> settings.copy(profile = UserProfile.CUSTOM, displayMode = event.value)
    is AppearanceEvent.Detailed -> settings.copy(profile = UserProfile.CUSTOM, detailedGuidance = event.enabled)
}

@Composable
internal fun SettingsAppearanceControls(settings: UserSettings, onEvent: (AppearanceEvent) -> Unit) {
    val contrastName = stringResource(R.string.appearance_contrast)
    val detailName = stringResource(R.string.appearance_detail)
    Text(stringResource(R.string.appearance_contrast_status, stringResource(if (settings.highContrast) R.string.backup_on else R.string.backup_off)))
    Switch(modifier = Modifier.semantics { contentDescription = contrastName }, checked = settings.highContrast,
        onCheckedChange = { onEvent(AppearanceEvent.Contrast(it)) })
    Text(stringResource(R.string.appearance_mode_status, displayModeLabel(settings.displayMode)))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.gapSmall), verticalArrangement = Arrangement.spacedBy(DesignTokens.gapSmall)) {
        DisplayMode.entries.forEach { mode ->
            val title = displayModeLabel(mode)
            val label = if (settings.displayMode == mode) stringResource(R.string.appearance_mode_selected, title) else title
            OutlinedButton(onClick = { onEvent(AppearanceEvent.Mode(mode)) }, modifier = Modifier.sizeIn(minHeight = DesignTokens.touchMinimum)) { Text(label) }
        }
    }
    Text(stringResource(R.string.appearance_detail_status, stringResource(if (settings.detailedGuidance) R.string.backup_detailed else R.string.backup_simple)))
    Switch(modifier = Modifier.semantics { contentDescription = detailName }, checked = settings.detailedGuidance,
        onCheckedChange = { onEvent(AppearanceEvent.Detailed(it)) })
    Text(stringResource(R.string.appearance_detail_explanation))
}

@Composable
private fun displayModeLabel(mode: DisplayMode): String = stringResource(when (mode) {
    DisplayMode.SYSTEM -> R.string.appearance_mode_system
    DisplayMode.DAY -> R.string.appearance_mode_day
    DisplayMode.NIGHT -> R.string.appearance_mode_night
})
