@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.designsystem.theme.SignKoreanShapes
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel

data class OnboardingChoiceUiState(
    val selectedLevel: LearningLevel? = null,
    val selectedInterests: Set<Interest> = emptySet(),
    val isSaving: Boolean = false,
    val hasSaveError: Boolean = false,
    val isCompleted: Boolean = false,
) {
    val canContinueFromLevel: Boolean
        get() = selectedLevel != null
}

sealed interface OnboardingChoiceAction {
    data class SelectLevel(
        val level: LearningLevel,
    ) : OnboardingChoiceAction

    data object SkipLevel : OnboardingChoiceAction

    data class ToggleInterest(
        val interest: Interest,
    ) : OnboardingChoiceAction

    data object ContinueFromLevel : OnboardingChoiceAction

    data object StartLearning : OnboardingChoiceAction
}

class OnboardingChoiceReducer {
    fun reduce(
        state: OnboardingChoiceUiState,
        action: OnboardingChoiceAction,
    ): OnboardingChoiceUiState =
        when (action) {
            is OnboardingChoiceAction.SelectLevel -> state.copy(selectedLevel = action.level, hasSaveError = false)

            OnboardingChoiceAction.SkipLevel -> state.copy(selectedLevel = LearningLevel.BEGINNER, hasSaveError = false)

            is OnboardingChoiceAction.ToggleInterest -> state.toggleInterest(action.interest)

            OnboardingChoiceAction.ContinueFromLevel,
            OnboardingChoiceAction.StartLearning,
            -> state
        }

    private fun OnboardingChoiceUiState.toggleInterest(interest: Interest): OnboardingChoiceUiState =
        copy(
            selectedInterests =
                if (interest in selectedInterests) {
                    selectedInterests - interest
                } else {
                    selectedInterests + interest
                },
            hasSaveError = false,
        )
}

object OnboardingChoiceTestTags {
    const val LEVEL_CONTINUE = "level-continue"
    const val LEVEL_SKIP = "level-skip"
    const val INTERESTS_START = "interests-start"
}

@Composable
fun LevelScreen(
    state: OnboardingChoiceUiState,
    onAction: (OnboardingChoiceAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().testTag(OnboardingStep.Level.screenId)) {
        OnboardingProgressIndicator(
            activeSteps = 3,
            modifier = Modifier.padding(top = SignKoreanSpacing.small),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = SignKoreanSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
        ) {
            Text(
                text = stringResource(R.string.onboarding_level_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.onboarding_level_description),
                style = MaterialTheme.typography.bodyLarge,
            )
            LearningLevel.entries.forEach { level ->
                ChoiceCard(
                    title = level.title(),
                    description = level.description(),
                    selected = state.selectedLevel == level,
                    onClick = { onAction(OnboardingChoiceAction.SelectLevel(level)) },
                    modifier = Modifier.fillMaxWidth().testTag("level-${level.name.lowercase()}"),
                )
            }
            Text(
                text = stringResource(R.string.onboarding_level_skip),
                modifier = Modifier.clickable { onAction(OnboardingChoiceAction.SkipLevel) }.testTag(OnboardingChoiceTestTags.LEVEL_SKIP),
                style = MaterialTheme.typography.labelLarge,
                color = Color(SignKoreanPalette.MUTED),
            )
        }
        GameButton(
            text = stringResource(R.string.onboarding_continue),
            enabled = state.canContinueFromLevel,
            onClick = { onAction(OnboardingChoiceAction.ContinueFromLevel) },
            modifier = Modifier.fillMaxWidth().testTag(OnboardingChoiceTestTags.LEVEL_CONTINUE),
        )
    }
}

@Composable
fun InterestsScreen(
    state: OnboardingChoiceUiState,
    onAction: (OnboardingChoiceAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().testTag(OnboardingStep.Interests.screenId)) {
        OnboardingProgressIndicator(
            activeSteps = 4,
            modifier = Modifier.padding(top = SignKoreanSpacing.small),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = SignKoreanSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
        ) {
            Text(
                text = stringResource(R.string.onboarding_interests_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.onboarding_interests_description),
                style = MaterialTheme.typography.bodyLarge,
            )
            Interest.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
                    row.forEach { interest ->
                        ChoiceCard(
                            title = interest.title(),
                            description = null,
                            selected = interest in state.selectedInterests,
                            onClick = { onAction(OnboardingChoiceAction.ToggleInterest(interest)) },
                            modifier = Modifier.weight(1f).testTag("interest-${interest.name.lowercase()}"),
                            role = Role.Checkbox,
                        )
                    }
                    if (row.size == 1) {
                        androidx.compose.foundation.layout
                            .Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (state.hasSaveError) {
                Text(
                    text = stringResource(R.string.onboarding_save_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        GameButton(
            text = stringResource(R.string.onboarding_start_learning),
            enabled = !state.isSaving && !state.isCompleted,
            onClick = { onAction(OnboardingChoiceAction.StartLearning) },
            modifier = Modifier.fillMaxWidth().testTag(OnboardingChoiceTestTags.INTERESTS_START),
        )
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    description: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.RadioButton,
) {
    Surface(
        modifier =
            modifier
                .semantics {
                    this.role = role
                    this.selected = selected
                }.clickable(onClick = onClick),
        color = if (selected) Color(SignKoreanPalette.LILAC) else Color(SignKoreanPalette.SURFACE),
        contentColor = Color(SignKoreanPalette.INK),
        shape = SignKoreanShapes.medium,
        border = BorderStroke(2.dp, Color(SignKoreanPalette.OUTLINE)),
    ) {
        Column(
            modifier = Modifier.padding(SignKoreanSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.xxSmall),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            description?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun LearningLevel.title(): String =
    stringResource(
        when (this) {
            LearningLevel.BEGINNER -> R.string.onboarding_level_beginner_title
            LearningLevel.ELEMENTARY -> R.string.onboarding_level_elementary_title
            LearningLevel.NOT_SURE -> R.string.onboarding_level_not_sure_title
        },
    )

@Composable
private fun LearningLevel.description(): String =
    stringResource(
        when (this) {
            LearningLevel.BEGINNER -> R.string.onboarding_level_beginner_description
            LearningLevel.ELEMENTARY -> R.string.onboarding_level_elementary_description
            LearningLevel.NOT_SURE -> R.string.onboarding_level_not_sure_description
        },
    )

@Composable
private fun Interest.title(): String =
    stringResource(
        when (this) {
            Interest.FOOD -> R.string.onboarding_interest_food
            Interest.TRANSPORT -> R.string.onboarding_interest_transport
            Interest.SHOPPING -> R.string.onboarding_interest_shopping
            Interest.LANDMARK -> R.string.onboarding_interest_landmark
            Interest.NEIGHBORHOOD -> R.string.onboarding_interest_neighborhood
            Interest.MARKET -> R.string.onboarding_interest_market
        },
    )
