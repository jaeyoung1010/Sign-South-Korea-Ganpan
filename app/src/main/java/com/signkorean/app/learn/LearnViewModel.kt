package com.signkorean.app.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.model.LearnSessionError
import com.signkorean.domain.model.LoadLearnSessionResult
import com.signkorean.domain.port.input.LoadLearnSession
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
        private val photoPrefetcher: LearnPhotoPrefetcher = LearnPhotoPrefetcher { },
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<LearnSessionUiState>(LearnSessionUiState.Loading)
        private var loadJob: Job? = null
        private var revealJob: Job? = null
        private var isRevealing = false

        val uiState = mutableUiState.asStateFlow()

        fun load() {
            loadJob?.cancel()
            revealJob?.cancel()
            isRevealing = false
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
    }

private fun LoadLearnSessionResult.toUiState(): LearnSessionUiState =
    when (this) {
        is LoadLearnSessionResult.Active -> {
            LearnSessionUiState.Active(
                cardId = card.id,
                sign = card.korean,
                position = position,
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
