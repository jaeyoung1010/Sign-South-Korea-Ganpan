@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingChoicesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun level_requires_one_selection_and_skip_selects_beginner() {
        var currentState: OnboardingChoiceUiState? = null
        composeRule.setContent { LevelUnderTest(onStateChanged = { currentState = it }) }

        composeRule.onNodeWithTag(OnboardingChoiceTestTags.LEVEL_CONTINUE).assertIsNotEnabled()
        composeRule.onNodeWithTag("level-elementary").performClick()
        composeRule.onNodeWithTag(OnboardingChoiceTestTags.LEVEL_CONTINUE).assertIsEnabled()
        composeRule.runOnIdle { assertEquals(LearningLevel.ELEMENTARY, currentState?.selectedLevel) }

        composeRule.onNodeWithTag(OnboardingChoiceTestTags.LEVEL_SKIP).performClick()
        composeRule.runOnIdle { assertEquals(LearningLevel.BEGINNER, currentState?.selectedLevel) }
    }

    @Test
    fun interests_allow_zero_or_multiple_selections() {
        var currentState: OnboardingChoiceUiState? = null
        composeRule.setContent { InterestsUnderTest(onStateChanged = { currentState = it }) }

        composeRule.onNodeWithTag(OnboardingChoiceTestTags.INTERESTS_CONTINUE).assertIsEnabled()
        composeRule.onNodeWithTag("interest-food").performClick()
        composeRule.onNodeWithTag("interest-transport").performClick()
        composeRule.runOnIdle {
            assertEquals(setOf(Interest.FOOD, Interest.TRANSPORT), currentState?.selectedInterests)
        }
    }

    @Composable
    private fun LevelUnderTest(onStateChanged: (OnboardingChoiceUiState) -> Unit) {
        var state by remember { mutableStateOf(OnboardingChoiceUiState()) }
        LevelScreen(
            state = state,
            onAction = { action ->
                state = OnboardingChoiceReducer().reduce(state, action)
                onStateChanged(state)
            },
        )
    }

    @Composable
    private fun InterestsUnderTest(onStateChanged: (OnboardingChoiceUiState) -> Unit) {
        var state by remember { mutableStateOf(OnboardingChoiceUiState()) }
        SignKoreanTheme {
            InterestsScreen(
                state = state,
                onAction = { action ->
                    state = OnboardingChoiceReducer().reduce(state, action)
                    onStateChanged(state)
                },
            )
        }
    }
}
