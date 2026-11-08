package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt

/** Draft slider changes are committed only when interaction finishes. */
@Composable
internal fun SettingsTextSizeControl(percent: Int, onCommit: (Int) -> Unit) {
    var draft by remember(percent) { mutableFloatStateOf(percent.toFloat()) }
    val sliderLabel = stringResource(R.string.settings_text_slider_name)
    Text(stringResource(R.string.settings_text_current, percent))
    Text(stringResource(R.string.settings_text_profile_notice))
    Text(stringResource(R.string.settings_text_draft, draft.roundToInt()))
    Slider(value = draft, onValueChange = { draft = it }, onValueChangeFinished = { onCommit(draft.roundToInt()) },
        valueRange = 100f..200f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = sliderLabel })
    Text(stringResource(R.string.settings_text_drag_notice))
}
