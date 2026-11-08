package com.example.hyundiaboysranger

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.hyundiaboysranger.ui.theme.DesignTokens

/** A user-selected learning topic, not a signal, route result, or stored review. */
internal enum class PreparationConcern(val title: Int, val destination: ConcernDestination) {
    VISIBILITY(R.string.concern_visibility, ConcernDestination.DEFOG),
    WIPER(R.string.concern_wiper, ConcernDestination.WIPER),
    WARNING(R.string.concern_warning, ConcernDestination.WARNING_CATALOG),
}
internal enum class ConcernDestination { DEFOG, WIPER, WARNING_CATALOG }
internal data class ConcernUiState(val senior: Boolean)
internal data class ConcernSelected(val concern: PreparationConcern)

@Composable
internal fun ConcernPreparation(state: ConcernUiState, onEvent: (ConcernSelected) -> Unit) {
    Text(stringResource(R.string.concern_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(if (state.senior) R.string.concern_senior_guide else R.string.concern_beginner_guide))
    Text(stringResource(R.string.concern_disclaimer))
    PreparationConcern.entries.forEach { concern ->
        OutlinedButton(onClick = { onEvent(ConcernSelected(concern)) },
            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = if (state.senior) DesignTokens.seniorTouchMinimum else DesignTokens.touchMinimum)) {
            Text(stringResource(concern.title))
        }
    }
}
