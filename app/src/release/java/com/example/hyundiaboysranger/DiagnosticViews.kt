@file:Suppress("UNUSED_PARAMETER")
package com.example.hyundiaboysranger

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable fun DiagnosticEntry(onOpen: () -> Unit) = Unit
@Composable fun DiagnosticScreen(preferHighways: Boolean, onBack: () -> Unit) {
    OutlinedButton(onClick = onBack) { Text(stringResource(R.string.checklist_home)) }
}
@Composable fun NavigationDebugDetails(session: NavigationSession) = Unit
