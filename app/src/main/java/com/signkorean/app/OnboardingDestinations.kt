@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.app

import androidx.compose.runtime.Composable
import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.feature.onboarding.InterestsScreen
import com.signkorean.feature.onboarding.LevelScreen
import com.signkorean.feature.onboarding.OnboardingChoiceAction
import com.signkorean.feature.onboarding.OnboardingChoiceUiState

@Composable
internal fun LevelDestination(
    state: OnboardingChoiceUiState,
    onSelectLevel: (LearningLevel) -> Unit,
    onSkipLevel: () -> Unit,
    onContinue: () -> Unit,
) {
    LevelScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is OnboardingChoiceAction.SelectLevel -> onSelectLevel(action.level)
                OnboardingChoiceAction.SkipLevel -> {
                    onSkipLevel()
                    onContinue()
                }
                OnboardingChoiceAction.ContinueFromLevel -> if (state.canContinueFromLevel) onContinue()
                else -> Unit
            }
        },
    )
}

@Composable
internal fun InterestsDestination(
    state: OnboardingChoiceUiState,
    onToggleInterest: (Interest) -> Unit,
    onStartLearning: () -> Unit,
) {
    InterestsScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is OnboardingChoiceAction.ToggleInterest -> onToggleInterest(action.interest)
                OnboardingChoiceAction.StartLearning -> onStartLearning()
                else -> Unit
            }
        },
    )
}
