@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.signkorean.designsystem.theme.SignKoreanTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TutorialScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun show_answer_then_example_rating_enables_continue() {
        composeRule.setContent { TutorialUnderTest() }

        composeRule.onNodeWithText("What does this sign say?").assertIsDisplayed()
        composeRule.onNodeWithTag(TutorialTestTags.CONTINUE).assertIsNotEnabled()

        composeRule.onNodeWithTag(TutorialTestTags.SHOW_ANSWER).performClick()

        composeRule.onNodeWithText("분식").assertIsDisplayed()
        composeRule.onNodeWithTag(TutorialTestTags.GOT_IT).assertIsDisplayed()
        composeRule.onNodeWithTag(TutorialTestTags.REVIEW).assertIsDisplayed()
        composeRule.onNodeWithTag(TutorialTestTags.GOT_IT).performClick()
        composeRule.onNodeWithTag(TutorialTestTags.CONTINUE).assertIsEnabled()
    }

    @Test
    fun direction_copy_uses_got_it_then_review() {
        composeRule.setContent { TutorialUnderTest(initialState = TutorialUiState(isAnswerVisible = true)) }

        assertTrue(TutorialPracticeRating.entries.toList() == listOf(TutorialPracticeRating.GotIt, TutorialPracticeRating.Review))
        composeRule.onNodeWithText("← Got it").assertIsDisplayed()
        composeRule.onNodeWithText("Review →").assertIsDisplayed()
    }

    @Test
    fun continue_is_reported_only_after_the_example_is_rated() {
        var continueActions = 0
        composeRule.setContent {
            SignKoreanTheme {
                OnboardingScreen(
                    state = OnboardingUiState(OnboardingStep.Tutorial),
                    onAction = { if (it == OnboardingUiAction.Continue) continueActions += 1 },
                )
            }
        }

        composeRule.onNodeWithTag(TutorialTestTags.SHOW_ANSWER).performClick()
        composeRule.onNodeWithTag(TutorialTestTags.REVIEW).performClick()
        composeRule.onNodeWithTag(TutorialTestTags.CONTINUE).performClick()

        assertEquals(1, continueActions)
    }

    @Composable
    private fun TutorialUnderTest(initialState: TutorialUiState = TutorialUiState()) {
        var state by remember { mutableStateOf(initialState) }
        SignKoreanTheme {
            TutorialScreen(
                state = state,
                onAction = { state = TutorialReducer().reduce(state, it) },
            )
        }
    }
}
