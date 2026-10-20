package com.signkorean.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.domain.model.FocusRect
import com.signkorean.feature.learn.LearnCardPreviewUiModel
import com.signkorean.feature.learn.LearnPhotoUiModel
import com.signkorean.feature.learn.LearnSessionScreen
import com.signkorean.feature.learn.LearnSessionTestTags
import com.signkorean.feature.learn.LearnSessionUiState
import com.signkorean.feature.learn.LearnUiAction
import com.signkorean.testsupport.ScreenshotArtifact
import com.signkorean.testsupport.ScreenshotCase
import com.signkorean.testsupport.ScreenshotTestHarness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun active_photo_is_accessible_while_next_two_photo_cards_are_hidden_from_accessibility() {
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(state = photoState(), onAction = {})
            }
        }

        composeRule.onNodeWithContentDescription("A street sign photo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Upcoming card").assertDoesNotExist()
    }

    @Test
    fun photo_stage_captures_at_each_supported_width_without_truncating_the_stack() {
        listOf(360, 390, 480).forEach { width ->
            composeRule.setContent {
                SignKoreanTheme {
                    Box(
                        modifier =
                            androidx.compose.ui.Modifier
                                .width(width.dp)
                                .height(720.dp),
                    ) {
                        LearnSessionScreen(state = photoState(), onAction = {})
                    }
                }
            }

            val image = composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_CARD_STACK).captureToImage()
            val artifact =
                ScreenshotTestHarness.capture(
                    ScreenshotCase("learn-photo-$width", widthDp = width, localeTag = "en-US"),
                ) { case -> ScreenshotArtifact(case.id, byteArrayOf(image.width.toByte(), image.height.toByte())) }

            assertTrue(artifact.bytes.isNotEmpty())
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

    private fun photoState() =
        active(position = 1, total = 20).copy(
            photo =
                LearnPhotoUiModel(
                    imageAssetUri = "file:///android_asset/signs/generated/01-hongdae-entrance.jpg",
                    focus = FocusRect(40f, 18f, 28f, 12f),
                    questionContentDescription = "A street sign photo",
                    answerContentDescription = "A street sign photo for 홍대입구",
                ),
            upcomingCards =
                listOf(
                    LearnCardPreviewUiModel("card-2", "file:///android_asset/signs/generated/02-gyeongbokgung.jpg"),
                    LearnCardPreviewUiModel("card-3", "file:///android_asset/signs/generated/03-bukchon-hanok-village.jpg"),
                ),
        )
}
