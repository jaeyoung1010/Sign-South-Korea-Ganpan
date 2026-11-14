package com.signkorean.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.feature.progress.ProgressCardUiModel
import com.signkorean.feature.progress.ProgressCategoryUiModel
import com.signkorean.feature.progress.ProgressDayUiModel
import com.signkorean.feature.progress.ProgressScreen
import com.signkorean.feature.progress.ProgressTestTags
import com.signkorean.feature.progress.ProgressUiAction
import com.signkorean.feature.progress.ProgressUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populated_progress_displays_stats_week_category_and_recent_cards() {
        composeRule.setContent {
            SignKoreanTheme {
                ProgressScreen(populatedState(), onAction = {})
            }
        }

        composeRule.onNodeWithText("Your progress").assertIsDisplayed()
        composeRule.onNodeWithTag(ProgressTestTags.STAT_LEARNED).assertIsDisplayed()
        composeRule.onNodeWithTag(ProgressTestTags.WEEK_CHART).assertIsDisplayed()
        composeRule.onNodeWithTag(ProgressTestTags.CATEGORY_PROGRESS).assertIsDisplayed()
        composeRule.onNodeWithText("홍대입구").assertIsDisplayed()
    }

    @Test
    fun see_all_and_card_detail_expose_close_and_practice_actions() {
        val actions = mutableListOf<ProgressUiAction>()
        composeRule.setContent {
            var state by remember { mutableStateOf(populatedState()) }
            SignKoreanTheme {
                ProgressScreen(state, onAction = { action ->
                    actions += action
                    state =
                        when (action) {
                            ProgressUiAction.OpenReviewList -> {
                                state.copy(isReviewListVisible = true)
                            }

                            is ProgressUiAction.SelectReview -> {
                                state.copy(
                                    selectedReview = state.allReviews.first { it.cardId == action.cardId },
                                )
                            }

                            ProgressUiAction.CloseDetail -> {
                                state.copy(selectedReview = null)
                            }

                            else -> {
                                state
                            }
                        }
                })
            }
        }

        composeRule.onNodeWithTag(ProgressTestTags.SEE_ALL).performClick()
        composeRule.onNodeWithTag(ProgressTestTags.REVIEW_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(ProgressTestTags.reviewCard("card-1")).performClick()
        composeRule.onNodeWithTag(ProgressTestTags.REVIEW_DETAIL).assertIsDisplayed()
        composeRule.onNodeWithText("A lively neighborhood landmark.").assertIsDisplayed()
        composeRule.onNodeWithTag(ProgressTestTags.DETAIL_CLOSE).performClick()
        composeRule.onNodeWithTag(ProgressTestTags.REVIEW_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(ProgressTestTags.reviewCard("card-1")).assertIsDisplayed()
        composeRule.onNodeWithTag(ProgressTestTags.reviewCard("card-1")).performClick()
        composeRule.onNodeWithTag(ProgressTestTags.PRACTICE).performClick()

        composeRule.runOnIdle {
            assertEquals(ProgressUiAction.OpenReviewList, actions[0])
            assertEquals(ProgressUiAction.SelectReview("card-1"), actions[1])
            assertEquals(ProgressUiAction.CloseDetail, actions[2])
            assertEquals(ProgressUiAction.SelectReview("card-1"), actions[3])
            assertEquals(ProgressUiAction.PracticeReview("card-1"), actions[4])
        }
    }

    private fun populatedState() =
        ProgressUiState(
            isLoading = false,
            learnedCount = 12,
            masteredCount = 4,
            dueCount = 3,
            week = (1..7).map { ProgressDayUiModel("D$it", it) },
            categories = listOf(ProgressCategoryUiModel("NEIGHBORHOOD", "Neighborhood", 3, 5, 0.6f)),
            recentReviews = listOf(review()),
            allReviews = listOf(review()),
        )

    private fun review() =
        ProgressCardUiModel(
            cardId = "card-1",
            korean = "홍대입구",
            meaning = "Hongdae Entrance",
            pronunciation = "hongdae ipgu",
            streetNote = "A lively neighborhood landmark.",
            imageAssetUri = "file:///android_asset/card-1.jpg",
        )
}
