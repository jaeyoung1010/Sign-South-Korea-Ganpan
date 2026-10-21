package com.signkorean.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.domain.model.FocusRect
import com.signkorean.feature.learn.LearnAnswerUiModel
import com.signkorean.feature.learn.LearnCardStage
import com.signkorean.feature.learn.LearnImageViewer
import com.signkorean.feature.learn.LearnPhotoUiModel
import com.signkorean.feature.learn.LearnSessionUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearnImageViewerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun question_viewer_hides_the_answer_label_and_keeps_the_question_description() {
        composeRule.setContent {
            SignKoreanTheme {
                LearnImageViewer(state = activeState(), onClose = {})
            }
        }

        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_IMAGE_VIEWER).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Question sign photo").assertIsDisplayed()
        composeRule.onNodeWithText("홍대입구").assertDoesNotExist()
        composeRule.onNodeWithText("Close viewer").assertIsDisplayed()
    }

    @Test
    fun answer_viewer_shows_the_korean_label_and_answer_description() {
        composeRule.setContent {
            SignKoreanTheme {
                LearnImageViewer(
                    state = activeState().copy(stage = LearnCardStage.Answer),
                    onClose = {},
                )
            }
        }

        composeRule.onNodeWithText("홍대입구").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Answer sign photo").assertIsDisplayed()
        composeRule.onNodeWithText("What does this sign mean?").assertDoesNotExist()
    }

    @Test
    fun close_action_only_requests_close_and_leaves_the_card_state_with_the_caller() {
        val closeCount = mutableListOf<Unit>()
        composeRule.setContent {
            SignKoreanTheme {
                LearnImageViewer(state = activeState().copy(stage = LearnCardStage.Answer), onClose = { closeCount += Unit })
            }
        }

        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_IMAGE_VIEWER_CLOSE).performClick()

        composeRule.runOnIdle { assertEquals(1, closeCount.size) }
    }

    private fun activeState() =
        LearnSessionUiState.Active(
            cardId = "card-1",
            sign = "홍대입구",
            position = 1,
            totalCount = 20,
            photo =
                LearnPhotoUiModel(
                    imageAssetUri = "file:///android_asset/signs/generated/01-hongdae-entrance.jpg",
                    focus = FocusRect(40f, 18f, 28f, 12f),
                    questionContentDescription = "Question sign photo",
                    answerContentDescription = "Answer sign photo",
                ),
            answer = LearnAnswerUiModel.placeholder(),
        )
}
