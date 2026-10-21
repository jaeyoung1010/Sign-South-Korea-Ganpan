package com.signkorean.app.learn

import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.LearnSessionAnswer
import com.signkorean.domain.model.LearnSessionError
import com.signkorean.domain.model.LearnSessionPreview
import com.signkorean.domain.model.LoadLearnSessionResult
import com.signkorean.domain.model.LocalizedWordPart
import com.signkorean.domain.model.RatingInputSource
import com.signkorean.domain.model.SignCard
import com.signkorean.domain.port.input.LoadLearnSession
import com.signkorean.domain.port.input.PauseSession
import com.signkorean.domain.port.input.PauseSessionResult
import com.signkorean.domain.port.input.RateCard
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.input.RevealAnswer
import com.signkorean.domain.port.input.RevealAnswerResult
import com.signkorean.domain.port.input.UndoRating
import com.signkorean.domain.port.input.UndoRatingResult
import com.signkorean.feature.learn.LearnCardStage
import com.signkorean.feature.learn.LearnFeedbackUiState
import com.signkorean.feature.learn.LearnPhotoPrefetcher
import com.signkorean.feature.learn.LearnSessionUiState
import com.signkorean.testsupport.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LearnViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `maps active session position and progress into immutable UI state`() =
        runTest {
            val viewModel = LearnViewModel(LoadLearnSession { active(position = 3, total = 10) })

            viewModel.load()
            advanceUntilIdle()

            val state = assertActive(viewModel.uiState.value)
            assertEquals("표지판 3", state.sign)
            assertEquals(3, state.position)
            assertEquals(10, state.totalCount)
            assertEquals(.3f, state.progressFraction)
            assertEquals(LearnCardStage.Question, state.stage)
        }

    @Test
    fun `reveal persists before transitioning the active card and does so once`() =
        runTest {
            var revealCount = 0
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 5) },
                    revealAnswerUseCase =
                        RevealAnswer {
                            revealCount += 1
                            RevealAnswerResult.Revealed
                        },
                )
            viewModel.load()
            advanceUntilIdle()

            viewModel.revealAnswer()
            viewModel.revealAnswer()
            advanceUntilIdle()

            assertEquals(LearnCardStage.Answer, assertActive(viewModel.uiState.value).stage)
            assertEquals(1, revealCount)
        }

    @Test
    fun `maps localized answer details and an already revealed card into answer state`() =
        runTest {
            val viewModel =
                LearnViewModel(
                    LoadLearnSession {
                        active(position = 1, total = 5).copy(
                            answer =
                                LearnSessionAnswer(
                                    displayMeaning = "Hongdae Entrance",
                                    pronunciationGuide = "hongdae ipgu",
                                    wordParts = listOf(LocalizedWordPart("홍대", "Hongdae")),
                                    streetNote = "A long street note.",
                                    showRomanization = true,
                                ),
                            answerRevealed = true,
                        )
                    },
                )

            viewModel.load()
            advanceUntilIdle()

            val state = assertActive(viewModel.uiState.value)
            assertEquals(LearnCardStage.Answer, state.stage)
            assertEquals("Hongdae Entrance", state.answer.displayMeaning)
            assertEquals("hongdae ipgu", state.answer.pronunciationGuide)
            assertTrue(state.answer.showRomanization)
        }

    @Test
    fun `keeps the question visible when answer persistence rejects a stale card`() =
        runTest {
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 5) },
                    revealAnswerUseCase = RevealAnswer { RevealAnswerResult.Unavailable },
                )
            viewModel.load()
            advanceUntilIdle()

            viewModel.revealAnswer()
            advanceUntilIdle()

            assertEquals(LearnCardStage.Question, assertActive(viewModel.uiState.value).stage)
        }

    @Test
    fun `exposes a safe error state for invalid session content`() =
        runTest {
            val viewModel =
                LearnViewModel(
                    LoadLearnSession { LoadLearnSessionResult.Invalid(LearnSessionError.UnknownCard) },
                )

            viewModel.load()
            advanceUntilIdle()

            assertEquals(LearnSessionUiState.Error(LearnSessionError.UnknownCard), viewModel.uiState.value)
        }

    @Test
    fun `clears a previous card while a new session load is in flight`() =
        runTest {
            val replacement = CompletableDeferred<LoadLearnSessionResult>()
            var calls = 0
            val viewModel =
                LearnViewModel(
                    LoadLearnSession {
                        calls += 1
                        if (calls == 1) active(position = 1, total = 5) else replacement.await()
                    },
                )
            viewModel.load()
            advanceUntilIdle()

            viewModel.load()
            runCurrent()

            assertEquals(LearnSessionUiState.Loading, viewModel.uiState.value)
            replacement.complete(active(position = 2, total = 5))
            advanceUntilIdle()
            assertEquals(2, assertActive(viewModel.uiState.value).position)
        }

    @Test
    fun `turns an unexpected load failure into a safe placeholder`() =
        runTest {
            val viewModel = LearnViewModel(LoadLearnSession { error("database unavailable") })

            viewModel.load()
            advanceUntilIdle()

            assertEquals(LearnSessionUiState.Error(LearnSessionError.Unavailable), viewModel.uiState.value)
        }

    @Test
    fun `prefetches only the next two image previews after loading the active card`() =
        runTest {
            val prefetched = mutableListOf<List<String>>()
            val viewModel =
                LearnViewModel(
                    loadLearnSession =
                        LoadLearnSession {
                            active(position = 1, total = 20).copy(
                                upcomingCards =
                                    listOf(
                                        LearnSessionPreview("card-2", "asset://card-2"),
                                        LearnSessionPreview("card-3", "asset://card-3"),
                                        LearnSessionPreview("card-4", "asset://card-4"),
                                    ),
                            )
                        },
                    photoPrefetcher = LearnPhotoPrefetcher { prefetched += it },
                )

            viewModel.load()
            advanceUntilIdle()

            assertEquals(listOf(listOf("asset://card-2", "asset://card-3")), prefetched)
        }

    @Test
    fun `successful rating enters exit state and blocks duplicate input until animation finishes`() =
        runTest {
            var calls = 0
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 2).copy(answerRevealed = true) },
                    rateCardUseCase =
                        RateCard {
                            calls += 1
                            RateCardResult.Rated(nextPosition = 1, completed = false)
                        },
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.rateCard(RatingDecision.GotIt, RatingInputSource.Gesture)
            viewModel.rateCard(RatingDecision.Review, RatingInputSource.Keyboard)
            advanceUntilIdle()

            val state = assertActive(viewModel.uiState.value)
            assertTrue(state.isRating)
            assertEquals(RatingDecision.GotIt, state.ratingDecision)
            assertEquals(1, calls)

            viewModel.finishRatingAnimation()
            advanceUntilIdle()
            assertEquals(1, assertActive(viewModel.uiState.value).position)
        }

    @Test
    fun `failed rating stays on the card and a later input retries`() =
        runTest {
            var calls = 0
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 2).copy(answerRevealed = true) },
                    rateCardUseCase =
                        RateCard {
                            calls += 1
                            if (calls == 1) {
                                RateCardResult.Failed
                            } else {
                                RateCardResult.Rated(nextPosition = 1, completed = false)
                            }
                        },
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.rateCard(RatingDecision.Review, RatingInputSource.Keyboard)
            advanceUntilIdle()
            assertTrue(assertActive(viewModel.uiState.value).ratingError)

            viewModel.rateCard(RatingDecision.Review, RatingInputSource.Keyboard)
            advanceUntilIdle()
            assertTrue(assertActive(viewModel.uiState.value).isRating)
            assertEquals(2, calls)
        }

    @Test
    fun `rating feedback expires after four seconds and a new rating replaces the undo target`() =
        runTest {
            var calls = 0
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 2).copy(answerRevealed = true) },
                    rateCardUseCase =
                        RateCard {
                            calls += 1
                            RateCardResult.Rated(nextPosition = 1, completed = false)
                        },
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.rateCard(RatingDecision.GotIt, RatingInputSource.Gesture)
            runCurrent()

            assertEquals(LearnFeedbackUiState("Marked as Got it", canUndo = true), viewModel.feedbackState.value)
            viewModel.finishRatingAnimation()
            runCurrent()
            viewModel.rateCard(RatingDecision.Review, RatingInputSource.Keyboard)
            runCurrent()

            assertEquals(LearnFeedbackUiState("Marked as Review", canUndo = true), viewModel.feedbackState.value)
            assertEquals(2, calls)
            advanceTimeBy(4_000)
            runCurrent()
            assertNull(viewModel.feedbackState.value)
        }

    @Test
    fun `undo restores the previous answer card and replaces feedback`() =
        runTest {
            var undoCalls = 0
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 2).copy(answerRevealed = true) },
                    rateCardUseCase = RateCard { RateCardResult.Rated(nextPosition = 1, completed = false) },
                    undoRatingUseCase =
                        UndoRating {
                            undoCalls += 1
                            UndoRatingResult.Undone("card-1", 0, RatingDecision.Review)
                        },
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.rateCard(RatingDecision.Review, RatingInputSource.Gesture)
            runCurrent()
            viewModel.finishRatingAnimation()
            runCurrent()

            viewModel.undoRating()
            runCurrent()

            assertEquals(1, undoCalls)
            assertEquals(LearnFeedbackUiState("Last choice undone", canUndo = false), viewModel.feedbackState.value)
            assertEquals(LearnCardStage.Answer, assertActive(viewModel.uiState.value).stage)
        }

    @Test
    fun `rating input is blocked while undo is being persisted`() =
        runTest {
            val undoGate = CompletableDeferred<UndoRatingResult>()
            var rateCalls = 0
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 2).copy(answerRevealed = true) },
                    rateCardUseCase =
                        RateCard {
                            rateCalls += 1
                            RateCardResult.Rated(nextPosition = 1, completed = false)
                        },
                    undoRatingUseCase = UndoRating { undoGate.await() },
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.rateCard(RatingDecision.GotIt, RatingInputSource.Gesture)
            runCurrent()
            viewModel.finishRatingAnimation()
            runCurrent()
            viewModel.undoRating()
            runCurrent()

            viewModel.rateCard(RatingDecision.Review, RatingInputSource.Keyboard)
            runCurrent()
            assertEquals(1, rateCalls)

            undoGate.complete(UndoRatingResult.Undone("card-1", 0, RatingDecision.GotIt))
            runCurrent()
        }

    @Test
    fun `pause emits a navigation effect only after persistence succeeds`() =
        runTest {
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 2, total = 5) },
                    pauseSessionUseCase =
                        PauseSession {
                            assertEquals("session-5", it.sessionId)
                            PauseSessionResult.Paused(
                                com.signkorean.domain.model
                                    .ActiveSession(1, 5, "session-5", isPaused = true),
                            )
                        },
                )
            viewModel.load()
            advanceUntilIdle()

            val effect = async { viewModel.effects.first() }
            viewModel.pauseSession()
            advanceUntilIdle()

            assertEquals(LearnSessionEffect.SessionPaused, effect.await())
        }

    @Test
    fun `pause failure keeps the learn screen available`() =
        runTest {
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 5) },
                    pauseSessionUseCase = PauseSession { PauseSessionResult.Unavailable },
                )
            viewModel.load()
            advanceUntilIdle()

            val effect = async { viewModel.effects.first() }
            viewModel.pauseSession()
            advanceUntilIdle()

            assertEquals(LearnSessionEffect.PauseFailed, effect.await())
            assertTrue(viewModel.uiState.value is LearnSessionUiState.Active)
        }

    private fun active(
        position: Int,
        total: Int,
    ) = LoadLearnSessionResult.Active(
        sessionId = "session-$total",
        card =
            SignCard(
                id = "card-$position",
                order = position - 1,
                korean = "표지판 $position",
                category = "MARKET",
                imageAssetUri = "asset://card-$position",
                focus = FocusRect(0f, 0f, 1f, 1f),
                audioAssetUris = emptyMap(),
                contentVersion = 1,
            ),
        position = position,
        totalCount = total,
    )

    private fun assertActive(state: LearnSessionUiState): LearnSessionUiState.Active {
        assertTrue(state is LearnSessionUiState.Active)
        return state as LearnSessionUiState.Active
    }
}
