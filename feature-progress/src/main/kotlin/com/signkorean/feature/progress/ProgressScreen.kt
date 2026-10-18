@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.progress

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.signkorean.designsystem.components.GameCard

data class ProgressUiState(
    val title: String = "Your sign journey",
    val summary: String = "Progress appears here as you practice sign decks.",
)

sealed interface ProgressUiAction {
    data object ReturnToLearn : ProgressUiAction
}

sealed interface ProgressUiEffect {
    data object NavigateToLearn : ProgressUiEffect
}

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onAction: (ProgressUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    GameCard(modifier = modifier.testTag("screen-progress")) {
        Text(text = state.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = state.summary, style = MaterialTheme.typography.bodyLarge)
    }
}
