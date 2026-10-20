package com.signkorean.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.feature.learn.LearnSessionScreen
import com.signkorean.feature.learn.LearnSessionTestTags
import com.signkorean.feature.learn.LearnSessionUiState
import com.signkorean.feature.learn.LearnUiAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearnSessionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun active_session_displays_its_one_based_position_and_progress_bar() {
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(state = active(position = 3, total = 10), onAction = {})
            }
        }

        composeRule.onNodeWithText("3 of 10").assertIsDisplayed()
        composeRule.onNodeWithTag(LearnSessionTestTags.PROGRESS).assertIsDisplayed()
    }

    @Test
    fun card_index_updates_when_the_session_state_changes() {
        var sessionState by mutableStateOf(active(position = 1, total = 5))
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(state = sessionState, onAction = {})
            }
        }

        composeRule.onNodeWithText("1 of 5").assertIsDisplayed()
        sessionState = active(position = 2, total = 5)

        composeRule.onNodeWithText("2 of 5").assertIsDisplayed()
        composeRule.onNodeWithText("표지판 2").assertIsDisplayed()
    }

    @Test
    fun close_and_more_actions_are_exposed_from_the_top_bar() {
        val actions = mutableListOf<LearnUiAction>()
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(state = active(position = 1, total = 20), onAction = actions::add)
            }
        }

        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_CLOSE_SESSION).performClick()
        composeRule.onNodeWithTag(LearnSessionTestTags.MORE).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(LearnUiAction.CloseSession, LearnUiAction.ShowSessionMenu), actions)
        }
    }

    private fun active(
        position: Int,
        total: Int,
    ) = LearnSessionUiState.Active(
        cardId = "card-$position",
        sign = "표지판 $position",
        position = position,
        totalCount = total,
    )
}
