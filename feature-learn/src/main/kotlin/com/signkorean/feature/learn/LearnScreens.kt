@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.learn

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.testing.SignKoreanTestTags

data class LearnHomeUiState(
    val title: String = "Today's Seoul signs",
    val subtitle: String = "Choose a deck and learn from real streets.",
)

data class LearnSessionUiState(
    val sign: String = "주차 금지",
    val translation: String = "No parking",
    val isAnswerVisible: Boolean = false,
)

data class CompletionUiState(
    val title: String = "Great memory!",
    val summary: String = "You finished this sign deck.",
)

sealed interface LearnUiAction {
    data object StartSession : LearnUiAction

    data object RevealAnswer : LearnUiAction

    data object ShowImageViewer : LearnUiAction

    data object ShowDeckSheet : LearnUiAction

    data object FinishSession : LearnUiAction

    data object OpenProgress : LearnUiAction

    data object OpenSettings : LearnUiAction
}

sealed interface LearnUiEffect {
    data object NavigateToSession : LearnUiEffect

    data object NavigateToCompletion : LearnUiEffect

    data object OpenFullScreenViewer : LearnUiEffect

    data object OpenDeckSheet : LearnUiEffect
}

@Composable
fun LearnHomeScreen(
    state: LearnHomeUiState,
    onAction: (LearnUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    GameCard(modifier = modifier.testTag("screen-home")) {
        Text(text = state.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = state.subtitle, style = MaterialTheme.typography.bodyLarge)
        GameButton("Start a sign deck", { onAction(LearnUiAction.StartSession) }, Modifier.fillMaxWidth())
        GameButton("Choose a deck", { onAction(LearnUiAction.ShowDeckSheet) }, Modifier.fillMaxWidth(), style = GameButtonStyle.Secondary)
        GameButton("Progress", { onAction(LearnUiAction.OpenProgress) }, Modifier.fillMaxWidth(), style = GameButtonStyle.Secondary)
        GameButton("Settings", { onAction(LearnUiAction.OpenSettings) }, Modifier.fillMaxWidth(), style = GameButtonStyle.Secondary)
    }
}

@Composable
fun LearnSessionScreen(
    state: LearnSessionUiState,
    onAction: (LearnUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    GameCard(modifier = modifier.testTag("screen-learn")) {
        Text(text = "Sign deck", style = MaterialTheme.typography.titleMedium)
        GameCard(modifier = Modifier.testTag(SignKoreanTestTags.LEARN_CARD_STACK)) {
            Text(text = state.sign, style = MaterialTheme.typography.displayMedium)
            if (state.isAnswerVisible) {
                Text(text = state.translation, style = MaterialTheme.typography.titleLarge)
            }
        }
        GameButton(
            text = if (state.isAnswerVisible) "Show photo" else "Show answer",
            onClick = { onAction(LearnUiAction.RevealAnswer) },
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_REVEAL_ANSWER),
        )
        GameButton(
            text = "View sign photo",
            onClick = { onAction(LearnUiAction.ShowImageViewer) },
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_ZOOM_IMAGE),
            style = GameButtonStyle.Secondary,
        )
        GameButton(
            text = "Finish practice",
            onClick = { onAction(LearnUiAction.FinishSession) },
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_CLOSE_SESSION),
            style = GameButtonStyle.GotIt,
        )
    }
}

@Composable
fun CompletionScreen(
    state: CompletionUiState,
    onAction: (LearnUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    GameCard(modifier = modifier.testTag("screen-done")) {
        Text(text = state.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = state.summary, style = MaterialTheme.typography.bodyLarge)
        GameButton("See progress", { onAction(LearnUiAction.OpenProgress) }, Modifier.fillMaxWidth())
    }
}
