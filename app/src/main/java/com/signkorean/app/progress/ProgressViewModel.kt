package com.signkorean.app.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.model.ProgressSnapshot
import com.signkorean.domain.port.input.LoadReviewCardDetail
import com.signkorean.domain.port.input.ObserveProgress
import com.signkorean.domain.port.input.ObserveReviewCards
import com.signkorean.domain.port.input.PracticeCard
import com.signkorean.domain.port.input.PracticeCardRequest
import com.signkorean.domain.port.input.ReviewCardDetailRequest
import com.signkorean.domain.port.input.ReviewPageRequest
import com.signkorean.domain.port.input.StartSessionResult
import com.signkorean.domain.port.output.AppLocalePort
import com.signkorean.feature.progress.ProgressCardUiModel
import com.signkorean.feature.progress.ProgressCategoryUiModel
import com.signkorean.feature.progress.ProgressDayUiModel
import com.signkorean.feature.progress.ProgressUiAction
import com.signkorean.feature.progress.ProgressUiEffect
import com.signkorean.feature.progress.ProgressUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel
    @Inject
    constructor(
        private val observeProgress: ObserveProgress,
        private val observeReviewCards: ObserveReviewCards,
        private val loadReviewCardDetail: LoadReviewCardDetail,
        private val practiceCard: PracticeCard,
        private val appLocalePort: AppLocalePort,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(ProgressUiState())
        private val effectChannel = Channel<ProgressUiEffect>(Channel.BUFFERED)
        private var progressJob: Job? = null
        private var reviewListJob: Job? = null

        val uiState = mutableUiState.asStateFlow()
        val effects = effectChannel.receiveAsFlow()

        init {
            refresh()
        }

        fun refresh() {
            progressJob?.cancel()
            progressJob = viewModelScope.launch {
                try {
                    observeProgress().collect { snapshot ->
                        val baseState = snapshot.toUiState(mutableUiState.value, appLocalePort.getCurrent())
                        mutableUiState.value = baseState.copy(recentReviews = snapshot.recentReviews.mapNotNull(::toCardUiModel))
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    mutableUiState.value = mutableUiState.value.copy(isLoading = false, errorMessage = "Progress is unavailable. Try again.")
                }
            }
        }

        fun openReviewList() {
            if (mutableUiState.value.isReviewListVisible) return
            mutableUiState.value = mutableUiState.value.copy(isReviewListVisible = true, errorMessage = null)
            loadReviewList()
        }

        private fun loadReviewList() {
            reviewListJob?.cancel()
            reviewListJob =
                viewModelScope.launch {
                    try {
                        observeReviewCards(ReviewPageRequest(limit = Int.MAX_VALUE)).collect { reviews ->
                            val cards = reviews.mapNotNull(::toCardUiModel)
                            mutableUiState.value = mutableUiState.value.copy(allReviews = cards, errorMessage = null)
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        mutableUiState.value = mutableUiState.value.copy(errorMessage = "Review cards are unavailable. Try again.")
                    }
                }
        }

        fun onAction(action: ProgressUiAction) {
            when (action) {
                ProgressUiAction.ReturnToLearn -> effectChannel.trySend(ProgressUiEffect.NavigateToLearn)
                ProgressUiAction.OpenReviewList -> openReviewList()
                ProgressUiAction.CloseReviewList -> mutableUiState.value = mutableUiState.value.copy(isReviewListVisible = false, selectedReview = null)
                is ProgressUiAction.SelectReview -> selectReview(action.cardId)
                ProgressUiAction.CloseDetail -> mutableUiState.value = mutableUiState.value.copy(selectedReview = null)
                is ProgressUiAction.PracticeReview -> practice(action.cardId)
                ProgressUiAction.Retry -> {
                    refresh()
                    if (mutableUiState.value.isReviewListVisible) loadReviewList()
                }
            }
        }

        private fun selectReview(cardId: String) {
            val review = (mutableUiState.value.allReviews + mutableUiState.value.recentReviews).firstOrNull { it.cardId == cardId }
            mutableUiState.value = mutableUiState.value.copy(selectedReview = review, errorMessage = null)
        }

        private fun practice(cardId: String) {
            if (mutableUiState.value.isPracticing) return
            mutableUiState.value = mutableUiState.value.copy(isPracticing = true, errorMessage = null)
            viewModelScope.launch {
                try {
                    when (practiceCard(PracticeCardRequest(cardId))) {
                        is StartSessionResult.Started,
                        is StartSessionResult.Resumed,
                        -> {
                            mutableUiState.value = mutableUiState.value.copy(isPracticing = false, selectedReview = null)
                            effectChannel.trySend(ProgressUiEffect.NavigateToLearn)
                        }

                        is StartSessionResult.ReplacementRequired -> {
                            mutableUiState.value =
                                mutableUiState.value.copy(
                                    isPracticing = false,
                                    errorMessage = "Finish your current session before practicing this sign.",
                                )
                        }

                        StartSessionResult.NoCardsAvailable -> {
                            mutableUiState.value = mutableUiState.value.copy(isPracticing = false, errorMessage = "This sign is not available right now.")
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    mutableUiState.value = mutableUiState.value.copy(isPracticing = false, errorMessage = "Could not start practice. Try again.")
                }
            }
        }

        private fun toCardUiModel(review: com.signkorean.domain.model.ProgressReview): ProgressCardUiModel? {
            val detail =
                runCatching {
                    loadReviewCardDetail(
                        ReviewCardDetailRequest(
                            cardId = review.cardId,
                            languageTag = supportedLanguageTag(),
                        ),
                    )
                }.getOrNull() ?: return null
            return ProgressCardUiModel(
                cardId = review.cardId,
                korean = detail.card.korean,
                meaning = detail.copy.displayMeaning,
                pronunciation = detail.copy.pronunciationGuide,
                streetNote = detail.copy.streetNote,
                imageAssetUri = detail.card.imageAssetUri,
                reviewedAtLabel = DateTimeFormatter.ISO_LOCAL_DATE.format(review.reviewedAt.atZone(ZoneId.systemDefault())),
            )
        }

        private fun supportedLanguageTag(): String =
            if (Locale.getDefault().language.equals("ja", ignoreCase = true)) "ja" else "en"
    }

private fun ProgressSnapshot.toUiState(
    previous: ProgressUiState,
    languageTag: String,
): ProgressUiState =
    previous.copy(
        isLoading = false,
        errorMessage = null,
        learnedCount = learnedCount,
        masteredCount = masteredCount,
        dueCount = dueCount,
        week =
            weeklyActivity.map {
                ProgressDayUiModel(
                    label = it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.forLanguageTag(languageTag)),
                    count = it.count,
                    dayOfWeek = it.date.dayOfWeek,
                )
            },
        categories = categoryProgress.map { ProgressCategoryUiModel(it.key, it.key.toCategoryLabel(), it.learnedCount, it.totalCount, it.ratio) },
    )

private fun String.toCategoryLabel(): String =
    lowercase(Locale.ENGLISH)
        .split('_')
        .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.ENGLISH) } }
