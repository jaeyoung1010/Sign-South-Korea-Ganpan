package com.signkorean.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.RatingInputSource
import com.signkorean.feature.learn.LearnAnswerUiModel
import com.signkorean.feature.learn.LearnCardPreviewUiModel
import com.signkorean.feature.learn.LearnCardStage
import com.signkorean.feature.learn.LearnPhotoUiModel
import com.signkorean.feature.learn.LearnSessionScreen
import com.signkorean.feature.learn.LearnSessionTestTags
import com.signkorean.feature.learn.LearnSessionUiState
import com.signkorean.feature.learn.LearnUiAction
import com.signkorean.feature.learn.LearnWordPartUiModel
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
        composeRule.runOnIdle {
            val positionConfig = composeRule.onNodeWithTag(LearnSessionTestTags.POSITION).fetchSemanticsNode().config
            val progressConfig = composeRule.onNodeWithTag(LearnSessionTestTags.PROGRESS).fetchSemanticsNode().config
            assertTrue(positionConfig.contains(SemanticsProperties.Heading))
            assertEquals("3 of 10", progressConfig[SemanticsProperties.StateDescription])
        }
    }

    @Test
    fun card_index_updates_when_the_session_state_changes() {
        var sessionState by mutableStateOf(active(position = 1, total = 5).copy(stage = LearnCardStage.Answer))
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(state = sessionState, onAction = {})
            }
        }

        composeRule.onNodeWithText("1 of 5").assertIsDisplayed()
        sessionState = active(position = 2, total = 5).copy(stage = LearnCardStage.Answer)

        composeRule.onNodeWithText("2 of 5").assertIsDisplayed()
        composeRule.onNodeWithText("표지판 2").assertIsDisplayed()
    }

    @Test
    fun question_shows_only_the_photo_and_question_before_reveal() {
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(state = answerState().copy(stage = LearnCardStage.Question), onAction = {})
            }
        }

        composeRule.onNodeWithText("What does this sign mean?").assertIsDisplayed()
        composeRule.onNodeWithText("홍대입구").assertDoesNotExist()
        composeRule.onNodeWithText("Hongdae Entrance").assertDoesNotExist()
        composeRule.onNodeWithText("Words").assertDoesNotExist()
        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_REVEAL_ANSWER).assertDoesNotExist()
    }

    @Test
    fun question_card_blocks_rating_custom_actions_until_the_answer_is_revealed() {
        val dispatchedActions = mutableListOf<LearnUiAction>()
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(
                    state = answerState().copy(stage = LearnCardStage.Question),
                    onAction = dispatchedActions::add,
                )
            }
        }

        val config = composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_ACTIVE_CARD).fetchSemanticsNode().config
        val customActions = if (config.contains(SemanticsActions.CustomActions)) config[SemanticsActions.CustomActions] else emptyList()

        assertEquals(listOf("Mark as Got it", "Mark for review"), customActions.map { it.label })
        assertTrue(customActions.all { it.action() })
        assertEquals(
            listOf(LearnUiAction.CheckAnswerFirst, LearnUiAction.CheckAnswerFirst),
            dispatchedActions,
        )
    }

    @Test
    fun answer_card_exposes_localized_rating_custom_actions_that_share_the_rating_transaction() {
        val actions = mutableListOf<LearnUiAction>()
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(state = answerState(), onAction = actions::add)
            }
        }

        val config = composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_ACTIVE_CARD).fetchSemanticsNode().config
        val customActions = if (config.contains(SemanticsActions.CustomActions)) config[SemanticsActions.CustomActions] else emptyList()

        assertEquals(listOf("Mark as Got it", "Mark for review"), customActions.map { it.label })
        assertTrue(customActions.first { it.label == "Mark as Got it" }.action())
        assertTrue(customActions.first { it.label == "Mark for review" }.action())
        assertEquals(
            listOf(
                LearnUiAction.RateCard(RatingDecision.GotIt, RatingInputSource.Accessibility),
                LearnUiAction.RateCard(RatingDecision.Review, RatingInputSource.Accessibility),
            ),
            actions,
        )
    }

    @Test
    fun answer_card_swipe_dispatches_a_rating_action() {
        val actions = mutableListOf<LearnUiAction>()
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(state = answerState(), onAction = actions::add)
            }
        }

        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_ACTIVE_CARD).performTouchInput { swipeLeft() }

        composeRule.runOnIdle {
            assertEquals(
                listOf(LearnUiAction.RateCard(RatingDecision.GotIt, RatingInputSource.Gesture)),
                actions,
            )
        }
    }

    @Test
    fun answer_shows_required_content_below_the_unchanged_photo() {
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(state = answerState(), onAction = {})
            }
        }

        composeRule.onNodeWithText("Answer").assertIsDisplayed()
        composeRule.onNodeWithText("홍대입구").assertIsDisplayed()
        composeRule.onNodeWithText("Hongdae Entrance").assertIsDisplayed()
        composeRule.onNodeWithText("hongdae ipgu").assertIsDisplayed()
        composeRule.onNodeWithText("Words").assertIsDisplayed()
        composeRule.onNodeWithText("Street note").assertIsDisplayed()
        composeRule.onNodeWithText("← Got it · Review →").assertIsDisplayed()
        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_REVEAL_ANSWER).assertDoesNotExist()
        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_PHOTO).assertIsDisplayed()
    }

    @Test
    fun answer_speaker_action_emits_play_audio() {
        val actions = mutableListOf<LearnUiAction>()
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(state = answerState(), onAction = actions::add)
            }
        }

        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_PLAY_AUDIO).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(LearnUiAction.PlayAudio), actions)
        }
    }

    @Test
    fun answer_hides_the_pronunciation_guide_when_romanization_is_off() {
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(
                    state = answerState().copy(answer = answerState().answer.copy(showRomanization = false)),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("hongdae ipgu").assertDoesNotExist()
        composeRule.onNodeWithText("Hongdae Entrance").assertIsDisplayed()
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

    @Test
    fun answer_screenshot_matrix_supports_width_locale_and_large_font_scale() {
        val widths = listOf(360, 390, 480)
        val locales = listOf("en", "ja")
        val fontScales = listOf(1f, 1.3f, 2f)

        widths.forEach { width ->
            locales.forEach { locale ->
                fontScales.forEach { fontScale ->
                    val case = ScreenshotCase("learn-answer-$locale-$width-${fontScale.toString().replace('.', '-')}", width, locale, fontScale)
                    composeRule.setContent {
                        CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = fontScale)) {
                            SignKoreanTheme {
                                Box(
                                    modifier =
                                        androidx.compose.ui.Modifier
                                            .width(width.dp)
                                            .height(720.dp),
                                ) {
                                    LearnSessionScreen(
                                        state = localizedAnswerState(locale),
                                        onAction = {},
                                    )
                                }
                            }
                        }
                    }

                    composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_PHOTO).assertIsDisplayed()
                    composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_INFO_PANEL).assertIsDisplayed()
                    composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_PLAY_AUDIO).assertIsDisplayed()
                    val image = composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_CARD_STACK).captureToImage()
                    val artifact =
                        ScreenshotTestHarness.capture(case) { screenshotCase ->
                            ScreenshotArtifact(
                                screenshotCase.id,
                                byteArrayOf(
                                    (image.width shr 8).toByte(),
                                    image.width.toByte(),
                                    (image.height shr 8).toByte(),
                                    image.height.toByte(),
                                ),
                            )
                        }
                    assertTrue(artifact.bytes.size >= 4)
                }
            }
        }
    }

    @Test
    fun landscape_compact_layout_keeps_answer_controls_available_at_200_percent_font() {
        val actions = mutableListOf<LearnUiAction>()
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                SignKoreanTheme {
                    Box(
                        modifier =
                            androidx.compose.ui.Modifier
                                .width(720.dp)
                                .height(390.dp),
                    ) {
                        LearnSessionScreen(state = localizedAnswerState("ja"), onAction = actions::add)
                    }
                }
            }
        }

        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_PHOTO).assertIsDisplayed()
        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_INFO_PANEL).assertIsDisplayed()
        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_PLAY_AUDIO).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(LearnUiAction.PlayAudio), actions)
        }
    }

    @Test
    fun tapping_the_card_photo_opens_the_full_screen_viewer_action() {
        val actions = mutableListOf<LearnUiAction>()
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(state = localizedAnswerState("en"), onAction = actions::add)
            }
        }

        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_PHOTO).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(LearnUiAction.ShowImageViewer), actions)
        }
    }

    @Test
    fun tapping_a_question_card_photo_reveals_the_answer() {
        val actions = mutableListOf<LearnUiAction>()
        composeRule.setContent {
            SignKoreanTheme {
                LearnSessionScreen(
                    state = localizedAnswerState("ja").copy(stage = LearnCardStage.Question),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_PHOTO).performClick()
        composeRule.runOnIdle { assertEquals(listOf(LearnUiAction.RevealAnswer), actions) }
    }

    @Test
    fun long_english_and_japanese_answer_content_keeps_the_photo_above_the_scrollable_information_panel() {
        listOf(
            "en" to
                "The gateway to Hongdae, a popular area for live music, cafés, shopping, and nightlife. " +
                "It can be busy at every hour of the day and night.",
            "ja" to
                "ライブハウスやカフェ、買い物でにぎわう弘大エリアの玄関口です。" +
                "週末は特に混雑するため、周囲の歩行者に注意しましょう。",
        ).forEach { (language, streetNote) ->
            composeRule.setContent {
                SignKoreanTheme {
                    Box(
                        modifier =
                            androidx.compose.ui.Modifier
                                .width(390.dp)
                                .height(720.dp),
                    ) {
                        LearnSessionScreen(
                            state = answerState().copy(answer = answerState().answer.copy(streetNote = streetNote)),
                            onAction = {},
                        )
                    }
                }
            }

            composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_PHOTO).assertIsDisplayed()
            composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_INFO_PANEL).assertIsDisplayed()
            val image = composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_CARD_STACK).captureToImage()
            val artifact =
                ScreenshotTestHarness.capture(
                    ScreenshotCase("learn-answer-long-$language", widthDp = 390, localeTag = language),
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

    private fun answerState() =
        photoState().copy(
            sign = "홍대입구",
            stage = LearnCardStage.Answer,
            answer =
                LearnAnswerUiModel(
                    displayMeaning = "Hongdae Entrance",
                    pronunciationGuide = "hongdae ipgu",
                    wordParts =
                        listOf(
                            LearnWordPartUiModel("홍대", "Hongdae"),
                            LearnWordPartUiModel("입구", "entrance"),
                        ),
                    streetNote = "The gateway to Hongdae, a popular area for live music, cafés, shopping, and nightlife.",
                    showRomanization = true,
                ),
        )

    private fun localizedAnswerState(locale: String) =
        if (locale == "ja") {
            answerState().copy(
                sign = "弘大入口",
                answer =
                    answerState().answer.copy(
                        displayMeaning = "弘大エリアの入口",
                        pronunciationGuide = "ホンデ イック",
                        streetNote = "ライブハウスやカフェ、買い物でにぎわう弘大エリアの玄関口です。週末は特に混雑します。",
                        wordParts =
                            listOf(
                                LearnWordPartUiModel("弘大", "ホンデ"),
                                LearnWordPartUiModel("入口", "入り口"),
                            ),
                    ),
            )
        } else {
            answerState()
        }
}
