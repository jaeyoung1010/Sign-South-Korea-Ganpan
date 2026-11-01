package com.signkorean.app.learn

import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.AudioPlayRequest
import com.signkorean.domain.model.AudioPlaybackFailure
import com.signkorean.domain.model.AudioPlaybackFailedEvent
import com.signkorean.domain.model.AudioPlaybackState
import com.signkorean.domain.model.AudioPlayedEvent
import com.signkorean.domain.model.CardReportReason
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.LearnSessionAnswer
import com.signkorean.domain.model.LearnSessionError
import com.signkorean.domain.model.LearnSessionPreview
import com.signkorean.domain.model.LoadLearnSessionResult
import com.signkorean.domain.model.LocalizedWordPart
import com.signkorean.domain.model.RatingInputSource
import com.signkorean.domain.model.SessionCardScore
import com.signkorean.domain.model.SessionCompletion
import com.signkorean.domain.model.SignCard
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.model.VoiceId
import com.signkorean.domain.port.input.LoadLearnSession
import com.signkorean.domain.port.input.LoadSessionCompletion
import com.signkorean.domain.port.input.LoadSessionCompletionRequest
import com.signkorean.domain.port.input.PauseSession
import com.signkorean.domain.port.input.PauseSessionResult
import com.signkorean.domain.port.input.RateCard
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.input.RevealAnswer
import com.signkorean.domain.port.input.RevealAnswerResult
import com.signkorean.domain.port.input.SubmitCardReport
import com.signkorean.domain.port.input.SubmitCardReportResult
import com.signkorean.domain.port.input.UndoRating
import com.signkorean.domain.port.input.UndoRatingResult
import com.signkorean.domain.port.output.AudioPlayerPort
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.testsupport.FakeUserSettingsRepository
import com.signkorean.feature.learn.LearnCardStage
import com.signkorean.feature.learn.LearnFeedbackMessage
import com.signkorean.feature.learn.LearnFeedbackUiState
import com.signkorean.feature.learn.LearnPhotoPrefetcher
import com.signkorean.feature.learn.LearnSessionUiState
import com.signkorean.testsupport.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `plays the default bundled voice as soon as the card loads`() =
        runTest {
            val audioPlayer = RecordingAudioPlayer()
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 2).copy(answerRevealed = false) },
                    audioPlayer = audioPlayer,
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.revealAnswer()
            advanceUntilIdle()
            viewModel.playAudio()

            assertEquals(
                listOf(
                    AudioPlayRequest(
                        cardId = "card-1",
                        voiceId = VoiceId.TYPECAST_EUNKYUNG,
                        assetUri = "file:///android_asset/audio/eunkyung/card-1.mp3",
                    ),
                ),
                audioPlayer.requests,
            )
        }

    @Test
    fun `romanization setting updates the visible answer without reloading the card`() =
        runTest {
            val repository = FakeUserSettingsRepository(UserSettings(showRomanization = false))
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 1).copy(answerRevealed = true) },
                    userSettingsRepository = repository,
                )

            viewModel.load()
            advanceUntilIdle()
            assertFalse(assertActive(viewModel.uiState.value).answer.showRomanization)

            repository.updateSettings { it.copy(showRomanization = true) }
            advanceUntilIdle()

            assertTrue(assertActive(viewModel.uiState.value).answer.showRomanization)
        }

    @Test
    fun `auto plays once for an answer instance and does not replay on reload`() =
        runTest {
            var loadCount = 0
            val audioPlayer = RecordingAudioPlayer()
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession {
                        loadCount += 1
                        active(position = 1, total = 1).copy(answerRevealed = loadCount > 1)
                    },
                    audioPlayer = audioPlayer,
                    userSettingsRepository = FakeUserSettingsRepository(UserSettings(autoPlayAudio = true)),
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.revealAnswer()
            advanceUntilIdle()
            viewModel.load()
            advanceUntilIdle()

            assertEquals(1, audioPlayer.requests.size)
            assertEquals(VoiceId.TYPECAST_EUNKYUNG, audioPlayer.requests.single().voiceId)
        }

    @Test
    fun `auto play failure does not interrupt answer state`() =
        runTest {
            val analytics = RecordingAnalyticsSink()
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 1) },
                    audioPlayer = ThrowingAudioPlayer(),
                    analyticsSink = analytics,
                    userSettingsRepository = FakeUserSettingsRepository(UserSettings(autoPlayAudio = true)),
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.revealAnswer()
            advanceUntilIdle()

            assertEquals(LearnCardStage.Answer, assertActive(viewModel.uiState.value).stage)
            assertTrue(analytics.audioPlayed.isEmpty())
        }

    @Test
    fun `records whether playback was requested automatically`() =
        runTest {
            val audioPlayer = RecordingAudioPlayer()
            val analytics = RecordingAnalyticsSink()
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 1) },
                    audioPlayer = audioPlayer,
                    analyticsSink = analytics,
                    userSettingsRepository = FakeUserSettingsRepository(UserSettings(autoPlayAudio = true)),
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.revealAnswer()
            advanceUntilIdle()
            viewModel.playAudio()

            assertEquals(
                listOf(
                    AudioPlayedEvent("card-1", VoiceId.TYPECAST_EUNKYUNG, autoPlay = true),
                    AudioPlayedEvent("card-1", VoiceId.TYPECAST_EUNKYUNG, autoPlay = false),
                ),
                analytics.audioPlayed,
            )
        }

    @Test
    fun `publishes a privacy safe audio failure effect`() =
        runTest {
            val audioPlayer = RecordingAudioPlayer()
            val analytics = RecordingAnalyticsSink()
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 1).copy(answerRevealed = true) },
                    audioPlayer = audioPlayer,
                    analyticsSink = analytics,
                )
            viewModel.load()
            advanceUntilIdle()
            val effect = async { viewModel.effects.first() }

            audioPlayer.state.value =
                AudioPlaybackState.Failed(
                    cardId = "card-1",
                    voiceId = VoiceId.TYPECAST_EUNKYUNG,
                    reason = AudioPlaybackFailure.Decode,
                )
            advanceUntilIdle()

            assertEquals(LearnSessionEffect.AudioFailed(AudioPlaybackFailure.Decode), effect.await())
            assertEquals(
                listOf(
                    AudioPlaybackFailedEvent(
                        cardId = "card-1",
                        voiceId = VoiceId.TYPECAST_EUNKYUNG,
                        reason = AudioPlaybackFailure.Decode,
                    ),
                ),
                analytics.audioFailures,
            )
        }

    @Test
    fun `stops audio before loading the next card`() =
        runTest {
            val audioPlayer = RecordingAudioPlayer()
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 2).copy(answerRevealed = true) },
                    audioPlayer = audioPlayer,
                )
            viewModel.load()
            advanceUntilIdle()
            val stopsAfterInitialLoad = audioPlayer.stopCount

            viewModel.playAudio()
            viewModel.load()
            advanceUntilIdle()

            assertEquals(stopsAfterInitialLoad + 1, audioPlayer.stopCount)
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
    fun `keeps the question visible until the learner reveals the answer`() =
        runTest {
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 5) },
                    revealAnswerUseCase = RevealAnswer { RevealAnswerResult.Unavailable },
                )
            viewModel.load()
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
    fun `keeps the current card visible while the next card is loading`() =
        runTest {
            val nextCard = CompletableDeferred<LoadLearnSessionResult>()
            var calls = 0
            val viewModel =
                LearnViewModel(
                    loadLearnSession =
                        LoadLearnSession {
                            calls += 1
                            if (calls == 1) active(position = 1, total = 2).copy(answerRevealed = true) else nextCard.await()
                        },
                    rateCardUseCase = RateCard { RateCardResult.Rated(nextPosition = 1, completed = false) },
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.rateCard(RatingDecision.GotIt, RatingInputSource.Gesture)
            advanceUntilIdle()

            viewModel.finishRatingAnimation()
            runCurrent()

            val loadingNext = assertActive(viewModel.uiState.value)
            assertTrue(loadingNext.isLoadingNext)
            assertTrue(loadingNext.isRating)
            assertEquals(RatingDecision.GotIt, loadingNext.ratingDecision)
            nextCard.complete(active(position = 2, total = 2).copy(answerRevealed = true))
            advanceUntilIdle()
            assertFalse(assertActive(viewModel.uiState.value).isLoadingNext)
        }

    @Test
    fun `accessibility rating uses the same toast and undo transaction as gesture rating`() =
        runTest {
            var receivedSource: RatingInputSource? = null
            var undoCalls = 0
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 2).copy(answerRevealed = true) },
                    rateCardUseCase =
                        RateCard {
                            receivedSource = it.source
                            RateCardResult.Rated(nextPosition = 1, completed = false)
                        },
                    undoRatingUseCase =
                        UndoRating {
                            undoCalls += 1
                            UndoRatingResult.Undone("card-1", 0, RatingDecision.Review)
                        },
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.rateCard(RatingDecision.Review, RatingInputSource.Accessibility)
            runCurrent()

            assertEquals(RatingInputSource.Accessibility, receivedSource)
            assertEquals(LearnFeedbackUiState(LearnFeedbackMessage.MarkedReview, canUndo = true), viewModel.feedbackState.value)

            viewModel.finishRatingAnimation()
            runCurrent()
            viewModel.undoRating()
            runCurrent()

            assertEquals(1, undoCalls)
            assertEquals(LearnFeedbackUiState(LearnFeedbackMessage.LastChoiceUndone, canUndo = false), viewModel.feedbackState.value)
        }

    @Test
    fun `final rating exposes completion only after the exit animation finishes`() =
        runTest {
            val completion =
                SessionCompletion(
                    sessionId = "session-2",
                    totalCount = 2,
                    gotItCount = 1,
                    reviewCount = 1,
                    scores = listOf(SessionCardScore("card-1", RatingDecision.GotIt), SessionCardScore("card-2", RatingDecision.Review)),
                    nextReviewAt = null,
                    durationMillis = 1_000,
                )
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 2, total = 2).copy(answerRevealed = true) },
                    rateCardUseCase = RateCard { RateCardResult.Rated(nextPosition = 2, completed = true, completion = completion) },
                )
            viewModel.load()
            advanceUntilIdle()
            val effect = async { viewModel.effects.first() }

            viewModel.rateCard(RatingDecision.Review, RatingInputSource.Gesture)
            advanceUntilIdle()
            assertTrue(assertActive(viewModel.uiState.value).isRating)

            viewModel.finishRatingAnimation()
            advanceUntilIdle()

            assertEquals(LearnSessionUiState.Completed, viewModel.uiState.value)
            assertEquals(completion, viewModel.completionState.value)
            assertEquals(LearnSessionEffect.SessionCompleted(completion), effect.await())
        }

    @Test
    fun `recovers a completed session after the active query is empty`() =
        runTest {
            val completion =
                SessionCompletion(
                    sessionId = "session-2",
                    totalCount = 2,
                    gotItCount = 2,
                    reviewCount = 0,
                    scores = listOf(SessionCardScore("card-1", RatingDecision.GotIt), SessionCardScore("card-2", RatingDecision.GotIt)),
                    nextReviewAt = null,
                    durationMillis = 2_000,
                )
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { LoadLearnSessionResult.Invalid(LearnSessionError.NoActiveSession) },
                    loadSessionCompletion =
                        LoadSessionCompletion { request: LoadSessionCompletionRequest ->
                            assertEquals("session-2", request.sessionId)
                            completion
                        },
                    savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("learn-session-id" to "session-2")),
                )
            val effect = async { viewModel.effects.first() }

            viewModel.load()
            advanceUntilIdle()

            assertEquals(LearnSessionUiState.Completed, viewModel.uiState.value)
            assertEquals(completion, viewModel.completionState.value)
            assertEquals(LearnSessionEffect.SessionCompleted(completion), effect.await())
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
    fun `failed rating exposes a retry action that repeats the same transaction`() =
        runTest {
            var calls = 0
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 2).copy(answerRevealed = true) },
                    rateCardUseCase =
                        RateCard {
                            calls += 1
                            if (calls == 1) RateCardResult.Unavailable else RateCardResult.Rated(nextPosition = 1, completed = false)
                        },
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.rateCard(RatingDecision.Review, RatingInputSource.Gesture)
            runCurrent()

            assertEquals(
                LearnFeedbackUiState(LearnFeedbackMessage.SaveFailed, canUndo = false, canRetry = true),
                viewModel.feedbackState.value,
            )
            viewModel.retryRating()
            advanceUntilIdle()

            assertEquals(2, calls)
            assertTrue(assertActive(viewModel.uiState.value).isRating)
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

            assertNull(viewModel.feedbackState.value)
            viewModel.finishRatingAnimation()
            runCurrent()
            viewModel.rateCard(RatingDecision.Review, RatingInputSource.Keyboard)
            runCurrent()

            assertEquals(LearnFeedbackUiState(LearnFeedbackMessage.MarkedReview, canUndo = true), viewModel.feedbackState.value)
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
            assertEquals(LearnFeedbackUiState(LearnFeedbackMessage.LastChoiceUndone, canUndo = false), viewModel.feedbackState.value)
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

    @Test
    fun `report submission is saved once and resets the form after success`() =
        runTest {
            var calls = 0
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 5) },
                    submitCardReportUseCase =
                        SubmitCardReport {
                            calls += 1
                            SubmitCardReportResult.Saved("report-1")
                        },
                )
            viewModel.load()
            advanceUntilIdle()

            viewModel.openReport()
            viewModel.selectReportReason(CardReportReason.WRONG_MEANING)
            viewModel.updateReportDetails("The meaning looks wrong.")
            val effect = async { viewModel.effects.first() }
            viewModel.submitReport()
            viewModel.submitReport()
            advanceUntilIdle()

            assertEquals(1, calls)
            assertEquals(
                com.signkorean.feature.learn
                    .ReportUiState(),
                viewModel.reportState.value,
            )
            assertEquals(LearnSessionEffect.ReportSubmitted, effect.await())
        }

    @Test
    fun `report storage failure keeps the form and exposes a retryable error`() =
        runTest {
            val viewModel =
                LearnViewModel(
                    loadLearnSession = LoadLearnSession { active(position = 1, total = 5) },
                    submitCardReportUseCase = SubmitCardReport { SubmitCardReportResult.Unavailable },
                )
            viewModel.load()
            advanceUntilIdle()
            viewModel.openReport()
            viewModel.selectReportReason(CardReportReason.PHOTO_PROBLEM)
            viewModel.updateReportDetails("The image is blurry.")

            viewModel.submitReport()
            advanceUntilIdle()

            assertEquals(com.signkorean.feature.learn.ReportStatus.Error, viewModel.reportState.value.status)
            assertEquals("The image is blurry.", viewModel.reportState.value.details)
            assertTrue(viewModel.reportState.value.canSubmit)
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
                audioAssetUris =
                    mapOf(
                        VoiceId.TYPECAST_EUNKYUNG to "file:///android_asset/audio/eunkyung/card-$position.mp3",
                    ),
                contentVersion = 1,
            ),
        position = position,
        totalCount = total,
    )

    private fun assertActive(state: LearnSessionUiState): LearnSessionUiState.Active {
        assertTrue(state is LearnSessionUiState.Active)
        return state as LearnSessionUiState.Active
    }

    private class RecordingAudioPlayer : AudioPlayerPort {
        val requests = mutableListOf<AudioPlayRequest>()
        val state = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
        var stopCount = 0

        override fun play(request: AudioPlayRequest) {
            requests += request
            state.value = AudioPlaybackState.Playing(request.cardId, request.voiceId)
        }

        override fun stop() {
            stopCount += 1
            state.value = AudioPlaybackState.Idle
        }

        override fun observePlayback() = state
    }

    private class ThrowingAudioPlayer : AudioPlayerPort {
        private val state = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)

        override fun play(request: AudioPlayRequest) {
            error("decoder unavailable")
        }

        override fun stop() = Unit

        override fun observePlayback() = state
    }

    private class RecordingAnalyticsSink : AnalyticsSink {
        val audioFailures = mutableListOf<AudioPlaybackFailedEvent>()
        val audioPlayed = mutableListOf<AudioPlayedEvent>()

        override fun recordAppStarted(event: com.signkorean.domain.model.AppStartupEvent) = Unit

        override fun recordAudioPlaybackFailed(event: AudioPlaybackFailedEvent) {
            audioFailures += event
        }

        override fun recordAudioPlayed(event: AudioPlayedEvent) {
            audioPlayed += event
        }
    }
}
