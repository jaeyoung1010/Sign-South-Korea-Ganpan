@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.designsystem.theme.SignKoreanShapes
import com.signkorean.designsystem.theme.SignKoreanSpacing

enum class TutorialPracticeRating {
    GotIt,
    Review,
}

data class TutorialUiState(
    val isAnswerVisible: Boolean = false,
    val practiceRating: TutorialPracticeRating? = null,
) {
    val canContinue: Boolean
        get() = isAnswerVisible && practiceRating != null
}

sealed interface TutorialAction {
    data object ShowAnswer : TutorialAction

    data object SelectGotIt : TutorialAction

    data object SelectReview : TutorialAction

    data object Continue : TutorialAction
}

class TutorialReducer {
    fun reduce(
        state: TutorialUiState,
        action: TutorialAction,
    ): TutorialUiState =
        when (action) {
            TutorialAction.ShowAnswer -> state.copy(isAnswerVisible = true)
            TutorialAction.SelectGotIt -> state.selectPracticeRating(TutorialPracticeRating.GotIt)
            TutorialAction.SelectReview -> state.selectPracticeRating(TutorialPracticeRating.Review)
            TutorialAction.Continue -> state
        }

    private fun TutorialUiState.selectPracticeRating(rating: TutorialPracticeRating): TutorialUiState =
        if (isAnswerVisible) copy(practiceRating = rating) else this
}

object TutorialTestTags {
    const val SHOW_ANSWER = "tutorial-show-answer"
    const val ANSWER = "tutorial-answer"
    const val GOT_IT = "tutorial-got-it"
    const val REVIEW = "tutorial-review"
    const val CONTINUE = "tutorial-continue"
}

@Composable
fun RememberedTutorialScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isAnswerVisible by rememberSaveable { mutableStateOf(false) }
    var practiceRatingName by rememberSaveable { mutableStateOf<String?>(null) }
    val reducer = remember { TutorialReducer() }
    val state =
        TutorialUiState(
            isAnswerVisible = isAnswerVisible,
            practiceRating = practiceRatingName?.let(TutorialPracticeRating::valueOf),
        )

    TutorialScreen(
        state = state,
        onAction = { action ->
            if (action == TutorialAction.Continue) {
                if (state.canContinue) onContinue()
            } else {
                val nextState = reducer.reduce(state, action)
                isAnswerVisible = nextState.isAnswerVisible
                practiceRatingName = nextState.practiceRating?.name
            }
        },
        modifier = modifier,
    )
}

@Composable
fun TutorialScreen(
    state: TutorialUiState,
    onAction: (TutorialAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().testTag(OnboardingStep.Tutorial.screenId),
    ) {
        OnboardingProgressIndicator(
            activeSteps = 2,
            modifier = Modifier.padding(top = SignKoreanSpacing.small),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = SignKoreanSpacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
        ) {
            Text(
                text = stringResource(R.string.onboarding_tutorial_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.onboarding_tutorial_description),
                style = MaterialTheme.typography.bodyLarge,
            )
            TutorialCard(state = state, onAction = onAction)
        }
        GameButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = { onAction(TutorialAction.Continue) },
            enabled = state.canContinue,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = SignKoreanSpacing.medium)
                    .testTag(TutorialTestTags.CONTINUE),
        )
    }
}

@Composable
private fun TutorialCard(
    state: TutorialUiState,
    onAction: (TutorialAction) -> Unit,
) {
    GameCard(modifier = Modifier.fillMaxWidth()) {
        TutorialSignImage()
        if (state.isAnswerVisible) {
            Column(
                modifier = Modifier.testTag(TutorialTestTags.ANSWER),
                verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_tutorial_korean),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = stringResource(R.string.onboarding_tutorial_meaning),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
                    GameButton(
                        text = stringResource(R.string.onboarding_tutorial_got_it),
                        onClick = { onAction(TutorialAction.SelectGotIt) },
                        modifier = Modifier.weight(1f).testTag(TutorialTestTags.GOT_IT),
                        style = GameButtonStyle.GotIt,
                    )
                    GameButton(
                        text = stringResource(R.string.onboarding_tutorial_review),
                        onClick = { onAction(TutorialAction.SelectReview) },
                        modifier = Modifier.weight(1f).testTag(TutorialTestTags.REVIEW),
                        style = GameButtonStyle.Review,
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.onboarding_tutorial_prompt),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            GameButton(
                text = stringResource(R.string.onboarding_tutorial_show_answer),
                onClick = { onAction(TutorialAction.ShowAnswer) },
                modifier = Modifier.fillMaxWidth().testTag(TutorialTestTags.SHOW_ANSWER),
            )
        }
    }
}

@Composable
private fun TutorialSignImage() {
    val bitmap = rememberAssetBitmap("signs/bunsik.jpg")
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(SignKoreanShapes.large),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            Box(modifier = Modifier.fillMaxSize().clip(SignKoreanShapes.large).background(Color(SignKoreanPalette.Y2K_YELLOW)))
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.onboarding_tutorial_image_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
