package com.signkorean.app.learn

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.LearnSessionError
import com.signkorean.domain.model.LoadLearnSessionResult
import com.signkorean.domain.model.RatingInputSource
import com.signkorean.domain.port.input.LoadLearnSession
import com.signkorean.domain.port.input.PauseSession
import com.signkorean.domain.port.input.PauseSessionRequest
import com.signkorean.domain.port.input.PauseSessionResult
import com.signkorean.domain.port.input.RateCard
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.input.RevealAnswer
import com.signkorean.domain.port.input.RevealAnswerResult
import com.signkorean.domain.port.input.UndoRating
import com.signkorean.domain.port.input.UndoRatingRequest
import com.signkorean.domain.port.input.UndoRatingResult
import com.signkorean.feature.learn.LearnAnswerUiModel
import com.signkorean.feature.learn.LearnCardPreviewUiModel
import com.signkorean.feature.learn.LearnCardStage
import com.signkorean.feature.learn.LearnFeedbackUiState
import com.signkorean.feature.learn.LearnPhotoPrefetcher
import com.signkorean.feature.learn.LearnPhotoUiModel
import com.signkorean.feature.learn.LearnSessionUiState
import com.signkorean.feature.learn.LearnWordPartUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearnViewModel
    @Inject
    constructor(
        private val loadLearnSession: LoadLearnSession,
        private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
        private val revealAnswerUseCase: RevealAnswer = RevealAnswer { RevealAnswerResult.Revealed },
        private val rateCardUseCase: RateCard = RateCard { RateCardResult.Unavailable },
        private val undoRatingUseCase: UndoRating = UndoRating { UndoRatingResult.Unavailable },
        private val pauseSessionUseCase: PauseSession = PauseSession { PauseSessionResult.Unavailable },
        private val photoPrefetcher: LearnPhotoPrefetcher = LearnPhotoPrefetcher { },
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<LearnSessionUiState>(LearnSessionUiState.Loading)
        private var loadJob: Job? = null
        private var revealJob: Job? = null
        private var ratingJob: Job? = null
        private var undoJob: Job? = null
        private var feedbackJob: Job? = null
        private var isRevealing = false
        private var isRating = false
        private var isUndoing = false
        private var lastRatedSessionId: String? = null
        private val mutableFeedbackState = MutableStateFlow<LearnFeedbackUiState?>(null)
        private val effectChannel = Channel<LearnSessionEffect>(Channel.BUFFERED)
        private var isPausing = false

        val uiState = mutableUiState.asStateFlow()
        val feedbackState = mutableFeedbackState.asStateFlow()
        val effects = effectChannel.receiveAsFlow()

        fun load(preserveFeedback: Boolean = false) {
            loadJob?.cancel()
            revealJob?.cancel()
            ratingJob?.cancel()
            if (!preserveFeedback) {
                undoJob?.cancel()
                isUndoing = false
                lastRatedSessionId = null
                clearFeedback()
            }
            isRevealing = false
            isRating = false
            mutableUiState.value = LearnSessionUiState.Loading
            loadJob =
                viewModelScope.launch {
                    try {
                        val result = loadLearnSession()
                        mutableUiState.value = result.toUiState()
                        if (result is LoadLearnSessionResult.Active) {
                            savedStateHandle[SESSION_ID_KEY] = result.sessionId
                            savedStateHandle[ANSWER_REVEALED_KEY] = result.answerRevealed
                            photoPrefetcher.prefetch(
                                result.upcomingCards.take(MAX_PREFETCH_COUNT).map { it.imageAssetUri },
                            )
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        mutableUiState.value = LearnSessionUiState.Error(LearnSessionError.Unavailable)
                    }
                }
        }

        fun revealAnswer() {
            val active = mutableUiState.value as? LearnSessionUiState.Active ?: return
            if (active.stage != LearnCardStage.Question || isRevealing) return
            isRevealing = true
            revealJob =
                viewModelScope.launch {
                    try {
                        when (revealAnswerUseCase()) {
                            RevealAnswerResult.Revealed,
                            RevealAnswerResult.AlreadyRevealed,
                            -> {
                                val current = mutableUiState.value as? LearnSessionUiState.Active ?: return@launch
                                if (current.cardId == active.cardId && current.stage == LearnCardStage.Question) {
                                    mutableUiState.value = current.copy(stage = LearnCardStage.Answer)
                                    savedStateHandle[ANSWER_REVEALED_KEY] = true
                                }
                            }

                            RevealAnswerResult.Unavailable -> {
                                Unit
                            }
                        }
                    } finally {
                        isRevealing = false
                    }
                }
        }

        fun rateCard(
            decision: RatingDecision,
            source: RatingInputSource,
        ) {
            val active = mutableUiState.value as? LearnSessionUiState.Active ?: return
            if (active.stage == LearnCardStage.Answer) {
                if (!active.isRating && !isRating && !isUndoing) {
                    if (active.sessionId.isNotBlank()) {
                        beginRating(active, decision, source)
                    }
                }
            }
        }

        private fun beginRating(
            active: LearnSessionUiState.Active,
            decision: RatingDecision,
            source: RatingInputSource,
        ) {
            isRating = true
            lastRatedSessionId = active.sessionId
            clearFeedback()
            ratingJob?.cancel()
            ratingJob =
                viewModelScope.launch {
                    val result =
                        try {
                            rateCardUseCase(
                                RateCardRequest(
                                    sessionId = active.sessionId,
                                    position = active.position - 1,
                                    cardId = active.cardId,
                                    decision = decision,
                                    source = source,
                                ),
                            )
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Exception) {
                            RateCardResult.Failed
                        }
                    when (result) {
                        is RateCardResult.Rated -> {
                            val current = mutableUiState.value as? LearnSessionUiState.Active
                            if (current?.cardId == active.cardId) {
                                mutableUiState.value =
                                    current.copy(
                                        isRating = true,
                                        ratingDecision = decision,
                                        ratingError = false,
                                    )
                                showFeedback(
                                    message =
                                        if (decision == RatingDecision.GotIt) {
                                            "Marked as Got it"
                                        } else {
                                            "Marked as Review"
                                        },
                                    canUndo = true,
                                )
                            }
                        }

                        RateCardResult.AlreadyRated,
                        RateCardResult.Unavailable,
                        RateCardResult.Failed,
                        -> {
                            isRating = false
                            val current = mutableUiState.value as? LearnSessionUiState.Active
                            if (current?.cardId == active.cardId) {
                                mutableUiState.value = current.copy(isRating = false, ratingError = true)
                            }
                        }
                    }
                }
        }

        fun finishRatingAnimation() {
            val active = mutableUiState.value as? LearnSessionUiState.Active ?: return
            if (!active.isRating) return
            isRating = false
            load(preserveFeedback = true)
        }

        fun undoRating() {
            val sessionId = lastRatedSessionId ?: return
            if (isUndoing || feedbackState.value?.canUndo != true) return
            isUndoing = true
            undoJob =
                viewModelScope.launch {
                    val result =
                        try {
                            undoRatingUseCase(UndoRatingRequest(sessionId))
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Exception) {
                            UndoRatingResult.Failed
                        }
                    when (result) {
                        is UndoRatingResult.Undone -> {
                            isUndoing = false
                            lastRatedSessionId = null
                            showFeedback("Last choice undone", canUndo = false)
                            load(preserveFeedback = true)
                        }

                        UndoRatingResult.NothingToUndo -> {
                            isUndoing = false
                            showFeedback("Nothing to undo", canUndo = false)
                        }

                        UndoRatingResult.Unavailable,
                        UndoRatingResult.Failed,
                        -> {
                            isUndoing = false
                            showFeedback("Couldn’t undo rating. Try again.", canUndo = true)
                        }
                    }
                }
        }

        fun pauseSession() {
            val active = mutableUiState.value as? LearnSessionUiState.Active ?: return
            if (isPausing || active.sessionId.isBlank()) return
            isPausing = true
            viewModelScope.launch {
                try {
                    when (pauseSessionUseCase(PauseSessionRequest(active.sessionId))) {
                        is PauseSessionResult.Paused,
                        is PauseSessionResult.AlreadyPaused,
                        -> effectChannel.trySend(LearnSessionEffect.SessionPaused)

                        PauseSessionResult.Unavailable -> effectChannel.trySend(LearnSessionEffect.PauseFailed)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    effectChannel.trySend(LearnSessionEffect.PauseFailed)
                } finally {
                    isPausing = false
                }
            }
        }

        private fun showFeedback(
            message: String,
            canUndo: Boolean,
        ) {
            feedbackJob?.cancel()
            mutableFeedbackState.value = LearnFeedbackUiState(message = message, canUndo = canUndo)
            feedbackJob =
                viewModelScope.launch {
                    delay(FEEDBACK_DURATION_MILLIS)
                    mutableFeedbackState.value = null
                }
        }

        private fun clearFeedback() {
            feedbackJob?.cancel()
            feedbackJob = null
            mutableFeedbackState.value = null
        }

        private companion object {
            const val SESSION_ID_KEY = "learn-session-id"
            const val ANSWER_REVEALED_KEY = "learn-answer-revealed"
        }
    }

sealed interface LearnSessionEffect {
    data object SessionPaused : LearnSessionEffect

    data object PauseFailed : LearnSessionEffect
}

private fun LoadLearnSessionResult.toUiState(): LearnSessionUiState =
    when (this) {
        is LoadLearnSessionResult.Active -> {
            LearnSessionUiState.Active(
                cardId = card.id,
                sign = card.korean,
                position = position,
                sessionId = sessionId,
                totalCount = totalCount,
                photo =
                    LearnPhotoUiModel(
                        imageAssetUri = photo.imageAssetUri,
                        focus = photo.focus,
                        questionContentDescription = photo.questionContentDescription,
                        answerContentDescription = photo.answerContentDescription,
                    ),
                answer =
                    LearnAnswerUiModel(
                        displayMeaning = answer.displayMeaning,
                        pronunciationGuide = answer.pronunciationGuide,
                        wordParts = answer.wordParts.map { LearnWordPartUiModel(it.korean, it.meaning) },
                        streetNote = answer.streetNote,
                        showRomanization = answer.showRomanization,
                    ),
                upcomingCards = upcomingCards.map { LearnCardPreviewUiModel(it.cardId, it.imageAssetUri) },
                stage = if (answerRevealed) LearnCardStage.Answer else LearnCardStage.Question,
            )
        }

        LoadLearnSessionResult.Completed -> {
            LearnSessionUiState.Completed
        }

        is LoadLearnSessionResult.Invalid -> {
            LearnSessionUiState.Error(error)
        }
    }

private const val MAX_PREFETCH_COUNT = 2
private const val FEEDBACK_DURATION_MILLIS = 4_000L
