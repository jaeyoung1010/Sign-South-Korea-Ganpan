package com.signkorean.app.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.LearnSessionError
import com.signkorean.domain.model.LoadLearnSessionResult
import com.signkorean.domain.model.RatingInputSource
import com.signkorean.domain.port.input.LoadLearnSession
import com.signkorean.domain.port.input.RateCard
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.input.RevealAnswer
import com.signkorean.domain.port.input.RevealAnswerResult
import com.signkorean.feature.learn.LearnAnswerUiModel
import com.signkorean.feature.learn.LearnCardPreviewUiModel
import com.signkorean.feature.learn.LearnCardStage
import com.signkorean.feature.learn.LearnPhotoPrefetcher
import com.signkorean.feature.learn.LearnPhotoUiModel
import com.signkorean.feature.learn.LearnSessionUiState
import com.signkorean.feature.learn.LearnWordPartUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearnViewModel
    @Inject
    constructor(
        private val loadLearnSession: LoadLearnSession,
        private val revealAnswerUseCase: RevealAnswer = RevealAnswer { RevealAnswerResult.Revealed },
        private val rateCardUseCase: RateCard = RateCard { RateCardResult.Unavailable },
        private val photoPrefetcher: LearnPhotoPrefetcher = LearnPhotoPrefetcher { },
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<LearnSessionUiState>(LearnSessionUiState.Loading)
        private var loadJob: Job? = null
        private var revealJob: Job? = null
        private var ratingJob: Job? = null
        private var isRevealing = false
        private var isRating = false

        val uiState = mutableUiState.asStateFlow()

        fun load() {
            loadJob?.cancel()
            revealJob?.cancel()
            ratingJob?.cancel()
            isRevealing = false
            isRating = false
            mutableUiState.value = LearnSessionUiState.Loading
            loadJob =
                viewModelScope.launch {
                    try {
                        val result = loadLearnSession()
                        mutableUiState.value = result.toUiState()
                        if (result is LoadLearnSessionResult.Active) {
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
            if (active.stage != LearnCardStage.Answer || active.isRating || isRating || active.sessionId.isBlank()) return
            isRating = true
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
                        } catch (_: Exception) {
                            RateCardResult.Failed
                        }
                    when (result) {
                        is RateCardResult.Rated -> {
                            val current = mutableUiState.value as? LearnSessionUiState.Active
                            if (current?.cardId == active.cardId) {
                                mutableUiState.value = current.copy(isRating = true, ratingDecision = decision, ratingError = false)
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
            load()
        }
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
