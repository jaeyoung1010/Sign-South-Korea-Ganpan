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
import com.signkorean.app.navigation.AppNavigationReducer
import com.signkorean.app.navigation.AppNavigationUiState
import com.signkorean.app.navigation.AppRoute
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.feature.learn.HomeCategoryUiModel
import com.signkorean.feature.learn.HomeSessionUiModel
import com.signkorean.feature.learn.HomeTestTags
import com.signkorean.feature.learn.LearnHomeScreen
import com.signkorean.feature.learn.LearnHomeUiState
import com.signkorean.feature.learn.LearnUiAction
import com.signkorean.feature.learn.WeekActivityUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearnHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home_displays_repository_backed_session_mix_categories_and_week() {
        var action: LearnUiAction? = null
        composeRule.setContent {
            SignKoreanTheme {
                LearnHomeScreen(
                    state = regularHomeState(),
                    onAction = { action = it },
                )
            }
        }

        composeRule.onNodeWithText("3 days").assertIsDisplayed()
        composeRule.onNodeWithText("20 street signs").assertIsDisplayed()
        composeRule.onNodeWithText("About 8 minutes").assertIsDisplayed()
        composeRule.onNodeWithText("3 review").assertIsDisplayed()
        composeRule.onNodeWithText("17 new").assertIsDisplayed()
        composeRule.onNodeWithText("Landmarks · 7 signs").assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.SEE_ALL).performClick()

        composeRule.runOnIdle { assertEquals(LearnUiAction.ShowDeckSheet, action) }
    }

    @Test
    fun active_session_replaces_start_copy_with_resume_progress() {
        composeRule.setContent {
            SignKoreanTheme {
                LearnHomeScreen(
                    state = regularHomeState().copy(session = HomeSessionUiModel.active(4, 10)),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("Pick up where you left off").assertIsDisplayed()
        composeRule.onNodeWithText("4 of 10 signs complete").assertIsDisplayed()
        composeRule.onNodeWithText("Resume session").assertIsDisplayed()
    }

    @Test
    fun see_all_opens_the_complete_repository_backed_category_sheet() {
        val reducer = AppNavigationReducer()
        composeRule.setContent {
            var navigationState by remember { mutableStateOf(AppNavigationUiState(backStack = listOf(AppRoute.Home))) }
            SignKoreanAppContent(
                state = navigationState,
                onAction = { navigationState = reducer.reduce(navigationState, it) },
                learnHomeState = regularHomeState(),
            )
        }

        composeRule.onNodeWithTag(HomeTestTags.SEE_ALL).performClick()

        composeRule.onNodeWithText("All categories").assertIsDisplayed()
        composeRule.onNodeWithTag("home-sheet-category-TRANSPORT").assertIsDisplayed()
    }

    private fun regularHomeState() =
        LearnHomeUiState(
            streakDays = 3,
            session = HomeSessionUiModel(length = 20, estimatedMinutes = 8, reviewCount = 3, newCount = 17),
            categories =
                listOf(
                    HomeCategoryUiModel("LANDMARK", "Landmarks", 7),
                    HomeCategoryUiModel("NEIGHBORHOOD", "Neighborhoods", 9),
                    HomeCategoryUiModel("MARKET", "Markets", 2),
                    HomeCategoryUiModel("TRANSPORT", "Transport", 2),
                ),
            week = (1..7).map { WeekActivityUiModel("D$it", it) },
        )
}
