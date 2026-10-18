@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameCard

enum class OnboardingStep(
    val screenId: String,
    val title: String,
    val description: String,
) {
    Welcome("screen-welcome", "Sign Korean", "Learn Korean from signs you encounter in Seoul."),
    Tutorial("screen-tutorial", "How it works", "Reveal a sign's meaning, then swipe to remember it."),
    Level("screen-level", "Choose your level", "Start with Korean signs you can almost read."),
    Interests("screen-interests", "Pick your places", "Choose the neighborhoods and places you want to explore."),
}

data class OnboardingUiState(
    val step: OnboardingStep,
)

sealed interface OnboardingUiAction {
    data object Continue : OnboardingUiAction

    data object GoBack : OnboardingUiAction
}

sealed interface OnboardingUiEffect {
    data object ContinueToNextStep : OnboardingUiEffect

    data object FinishOnboarding : OnboardingUiEffect
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onAction: (OnboardingUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    GameCard(modifier = modifier.testTag(state.step.screenId)) {
        Text(text = state.step.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = state.step.description, style = MaterialTheme.typography.bodyLarge)
        GameButton(
            text = if (state.step == OnboardingStep.Interests) "Start learning" else "Continue",
            onClick = { onAction(OnboardingUiAction.Continue) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
