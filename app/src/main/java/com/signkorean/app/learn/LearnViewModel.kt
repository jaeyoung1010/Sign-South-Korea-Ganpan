package com.signkorean.app.learn

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.AudioPlayRequest
import com.signkorean.domain.model.AudioPlaybackFailure
import com.signkorean.domain.model.AudioPlaybackFailedEvent
import com.signkorean.domain.model.AudioPlaybackState
import com.signkorean.domain.model.AudioPlayedEvent
import com.signkorean.domain.model.AudioVoice
import com.signkorean.domain.model.CardReportSavedEvent
import com.signkorean.domain.model.CardReportReason
import com.signkorean.domain.model.LearnSessionError
import com.signkorean.domain.model.LoadLearnSessionResult
import com.signkorean.domain.model.RatingInputSource
import com.signkorean.domain.model.ReportDetails
import com.signkorean.domain.model.SessionCompletion
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.model.VoiceId
import com.signkorean.domain.port.input.LoadLearnSession
import com.signkorean.domain.port.input.LoadSessionCompletion
import com.signkorean.domain.port.input.LoadSessionCompletionRequest
import com.signkorean.domain.port.input.PauseSession
import com.signkorean.domain.port.input.PauseSessionRequest
import com.signkorean.domain.port.input.PauseSessionResult
import com.signkorean.domain.port.input.RateCard
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.input.RevealAnswer
import com.signkorean.domain.port.input.RevealAnswerResult
import com.signkorean.domain.port.input.SubmitCardReport
import com.signkorean.domain.port.input.SubmitCardReportRequest
import com.signkorean.domain.port.input.SubmitCardReportResult
import com.signkorean.domain.port.input.UndoRating
import com.signkorean.domain.port.input.UndoRatingRequest
import com.signkorean.domain.port.input.UndoRatingResult
import com.signkorean.domain.port.output.AudioPlayerPort
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.feature.learn.LearnAnswerUiModel
import com.signkorean.feature.learn.LearnCardPreviewUiModel
import com.signkorean.feature.learn.LearnCardStage
import com.signkorean.feature.learn.LearnFeedbackMessage
import com.signkorean.feature.learn.LearnFeedbackUiState
import com.signkorean.feature.learn.LearnPhotoPrefetcher
import com.signkorean.feature.learn.LearnPhotoUiModel
import com.signkorean.feature.learn.LearnSessionUiState
import com.signkorean.feature.learn.LearnWordPartUiModel
import com.signkorean.feature.learn.ReportStatus
import com.signkorean.feature.learn.ReportUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearnViewModel
    @Inject
    constructor(
        private val loadLearnSession: LoadLearnSession,
        private val loadSessionCompletion: LoadSessionCompletion = LoadSessionCompletion { null },
        private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
        private val revealAnswerUseCase: RevealAnswer = RevealAnswer { RevealAnswerResult.Revealed },
        private val rateCardUseCase: RateCard = RateCard { RateCardResult.Unavailable },
        private val undoRatingUseCase: UndoRating = UndoRating { UndoRatingResult.Unavailable },
        private val pauseSessionUseCase: PauseSession = PauseSession { PauseSessionResult.Unavailable },
        private val submitCardReportUseCase: SubmitCardReport = SubmitCardReport { SubmitCardReportResult.Unavailable },
        private val photoPrefetcher: LearnPhotoPrefetcher = LearnPhotoPrefetcher { },
        private val audioPlayer: AudioPlayerPort = NoopAudioPlayer,
        private val analyticsSink: AnalyticsSink = AnalyticsSink { },
        private val userSettingsRepository: UserSettingsRepository = NoopUserSettingsRepository,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<LearnSessionUiState>(LearnSessionUiState.Loading)
        private var loadJob: Job? = null
        private var revealJob: Job? = null
        private var ratingJob: Job? = null
        private var undoJob: Job? = null
        private var feedbackJob: Job? = null
        private var reportJob: Job? = null
        private var isRevealing = false
        private var isRating = false
        private var isUndoing = false
        private var lastRatedSessionId: String? = null
        private val mutableFeedbackState = MutableStateFlow<LearnFeedbackUiState?>(null)
        private val effectChannel = Channel<LearnSessionEffect>(Channel.BUFFERED)
        private var isPausing = false
        private var pendingCompletion: SessionCompletion? = null
        private var pendingRating: PendingRating? = null
        private val mutableCompletionState = MutableStateFlow<SessionCompletion?>(null)
        private val mutableReportState = MutableStateFlow(ReportUiState())
        private val mutableAudioState = MutableStateFlow<AudioPlaybackState>(audioPlayer.observePlayback().value)
        private var isSubmittingReport = false
        private var userSettings = UserSettings()
        private var autoPlayedAnswerInstanceId: String? = null
        private val settingsReady = CompletableDeferred<Unit>()

        init {
            viewModelScope.launch {
                try {
                    userSettingsRepository.settings.collect { settings ->
                        val previousShowRomanization = userSettings.showRomanization
                        userSettings = settings
                        if (previousShowRomanization != settings.showRomanization) {
                            val active = mutableUiState.value as? LearnSessionUiState.Active
                            if (active != null) {
                                mutableUiState.value =
                                    active.copy(
                                        answer = active.answer.copy(showRomanization = settings.showRomanization),
                                    )
                            }
                        }
                        settingsReady.complete(Unit)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    settingsReady.complete(Unit)
                }
            }
            viewModelScope.launch {
                audioPlayer.observePlayback().collect { playbackState ->
                    mutableAudioState.value = playbackState
                    if (playbackState is AudioPlaybackState.Failed) {
                        runCatching {
                            analyticsSink.recordAudioPlaybackFailed(
                                AudioPlaybackFailedEvent(
                                    cardId = playbackState.cardId,
                                    voiceId = playbackState.voiceId,
                                    reason = playbackState.reason,
                                ),
                            )
                        }
                        effectChannel.trySend(LearnSessionEffect.AudioFailed(playbackState.reason))
                    }
                }
            }
        }

        val uiState = mutableUiState.asStateFlow()
        val feedbackState = mutableFeedbackState.asStateFlow()
        val completionState = mutableCompletionState.asStateFlow()
        val reportState = mutableReportState.asStateFlow()
        val audioState = mutableAudioState.asStateFlow()
        val effects = effectChannel.receiveAsFlow()

        fun load(preserveFeedback: Boolean = false) {
            stopAudio()
            loadJob?.cancel()
            revealJob?.cancel()
            ratingJob?.cancel()
            reportJob?.cancel()
            isSubmittingReport = false
            mutableReportState.value = ReportUiState()
            if (!preserveFeedback) {
                undoJob?.cancel()
                isUndoing = false
                lastRatedSessionId = null
                clearFeedback()
                pendingCompletion = null
                mutableCompletionState.value = null
                pendingRating = null
            }
            isRevealing = false
            isRating = false
            mutableUiState.value = LearnSessionUiState.Loading
            loadJob =
                viewModelScope.launch {
                    try {
                        settingsReady.await()
                        val result = loadLearnSession()
                        if (result is LoadLearnSessionResult.Invalid && result.error == LearnSessionError.NoActiveSession) {
                            val sessionId = savedStateHandle.get<String>(SESSION_ID_KEY)
                            if (sessionId != null) {
                                val completion = loadSessionCompletion(LoadSessionCompletionRequest(sessionId))
                                if (completion != null) {
                                    mutableCompletionState.value = completion
                                    mutableUiState.value = LearnSessionUiState.Completed
                                    effectChannel.trySend(LearnSessionEffect.SessionCompleted(completion))
                                    return@launch
                                }
                            }
                        }
                        val nextUiState = result.toUiState()
                        mutableUiState.value = nextUiState
                        if (nextUiState is LearnSessionUiState.Active && nextUiState.stage == LearnCardStage.Answer) {
                            maybeAutoPlay(nextUiState)
                        }
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
                                    val answerState = current.copy(stage = LearnCardStage.Answer)
                                    mutableUiState.value = answerState
                                    savedStateHandle[ANSWER_REVEALED_KEY] = true
                                    maybeAutoPlay(answerState)
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

        fun playAudio() {
            val active = mutableUiState.value as? LearnSessionUiState.Active ?: return
            if (active.stage != LearnCardStage.Answer) return
            playAudio(active, autoPlay = false)
        }

        private fun maybeAutoPlay(active: LearnSessionUiState.Active) {
            if (!userSettings.autoPlayAudio) return
            val answerInstanceId = "${active.sessionId}:${active.position}:${active.cardId}"
            if (autoPlayedAnswerInstanceId == answerInstanceId) return
            autoPlayedAnswerInstanceId = answerInstanceId
            playAudio(active, autoPlay = true)
        }

        private fun playAudio(
            active: LearnSessionUiState.Active,
            autoPlay: Boolean,
        ) {
            val voiceId = userSettings.audioVoice.toVoiceId()
            val request =
                AudioPlayRequest(
                    cardId = active.cardId,
                    voiceId = voiceId,
                    assetUri = active.audioAssetUris[voiceId].orEmpty(),
                )
            var playbackRequested = false
            try {
                audioPlayer.play(request)
                playbackRequested = true
            } catch (_: Exception) {
                // Audio failures are reported by the adapter through playback state when available.
            }
            if (playbackRequested) {
                try {
                    analyticsSink.recordAudioPlayed(AudioPlayedEvent(active.cardId, voiceId, autoPlay))
                } catch (_: Exception) {
                    // Analytics must never block learning or playback.
                }
            }
        }

        fun stopAudio() {
            audioPlayer.stop()
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

        fun retryRating() {
            val pending = pendingRating ?: return
            val active = mutableUiState.value as? LearnSessionUiState.Active ?: return
            if (
                active.sessionId != pending.sessionId ||
                active.position != pending.position ||
                active.cardId != pending.cardId ||
                active.stage != LearnCardStage.Answer
            ) {
                return
            }
            beginRating(active, pending.decision, pending.source)
        }

        private fun beginRating(
            active: LearnSessionUiState.Active,
            decision: RatingDecision,
            source: RatingInputSource,
        ) {
            pendingRating = PendingRating(active.sessionId, active.position, active.cardId, decision, source)
            stopAudio()
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
                            pendingRating = null
                            pendingCompletion = result.completion
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
                                            LearnFeedbackMessage.MarkedGotIt
                                        } else {
                                            LearnFeedbackMessage.MarkedReview
                                        },
                                    canUndo = true,
                                )
                            }
                        }

                        RateCardResult.AlreadyRated -> {
                            isRating = false
                            val current = mutableUiState.value as? LearnSessionUiState.Active
                            if (current?.cardId == active.cardId) {
                                mutableUiState.value = current.copy(isRating = false, ratingError = true)
                            }
                            showFeedback(LearnFeedbackMessage.SaveFailed, canUndo = false, canRetry = false)
                        }

                        RateCardResult.Unavailable,
                        RateCardResult.Failed,
                        -> {
                            isRating = false
                            val current = mutableUiState.value as? LearnSessionUiState.Active
                            if (current?.cardId == active.cardId) {
                                mutableUiState.value = current.copy(isRating = false, ratingError = true)
                            }
                            showFeedback(LearnFeedbackMessage.SaveFailed, canUndo = false, canRetry = true)
                        }
                    }
                }
        }

        fun finishRatingAnimation() {
            val active = mutableUiState.value as? LearnSessionUiState.Active ?: return
            if (!active.isRating) return
            isRating = false
            pendingCompletion?.let { completion ->
                pendingCompletion = null
                mutableCompletionState.value = completion
                mutableUiState.value = LearnSessionUiState.Completed
                clearFeedback()
                effectChannel.trySend(LearnSessionEffect.SessionCompleted(completion))
                return
            }
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
                            pendingCompletion = null
                            pendingRating = null
                            mutableCompletionState.value = null
                            showFeedback(LearnFeedbackMessage.LastChoiceUndone, canUndo = false)
                            load(preserveFeedback = true)
                        }

                        UndoRatingResult.NothingToUndo -> {
                            isUndoing = false
                            showFeedback(LearnFeedbackMessage.NothingToUndo, canUndo = false)
                        }

                        UndoRatingResult.Unavailable,
                        UndoRatingResult.Failed,
                        -> {
                            isUndoing = false
                            showFeedback(LearnFeedbackMessage.UndoFailed, canUndo = true)
                        }
                    }
                }
        }

        fun pauseSession() {
            val active = mutableUiState.value as? LearnSessionUiState.Active ?: return
            if (isPausing || active.sessionId.isBlank()) return
            stopAudio()
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

        fun openReport() {
            if (mutableUiState.value is LearnSessionUiState.Active) {
                mutableReportState.value = ReportUiState()
            }
        }

        fun selectReportReason(reason: CardReportReason) {
            if (isSubmittingReport) return
            mutableReportState.value =
                mutableReportState.value.copy(
                    selectedReason = reason,
                    status = ReportStatus.Editing,
                    errorMessage = null,
                )
        }

        fun updateReportDetails(details: String) {
            if (isSubmittingReport) return
            mutableReportState.value =
                mutableReportState.value
                    .withDetails(details)
                    .copy(status = ReportStatus.Editing, errorMessage = null)
        }

        fun cancelReport() {
            reportJob?.cancel()
            isSubmittingReport = false
            mutableReportState.value = ReportUiState()
        }

        fun submitReport() {
            val active = mutableUiState.value as? LearnSessionUiState.Active ?: return
            val report = mutableReportState.value
            val reason = report.selectedReason ?: return
            if (!report.canSubmit || isSubmittingReport) return
            isSubmittingReport = true
            mutableReportState.value = report.copy(status = ReportStatus.Saving, errorMessage = null)
            reportJob?.cancel()
            reportJob =
                viewModelScope.launch {
                    val result =
                        try {
                            submitCardReportUseCase(
                                SubmitCardReportRequest(
                                    cardId = active.cardId,
                                    reason = reason,
                                    details = ReportDetails(report.details),
                                ),
                            )
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Exception) {
                            SubmitCardReportResult.Unavailable
                        }
                    isSubmittingReport = false
                    when (result) {
                        is SubmitCardReportResult.Saved -> {
                            try {
                                analyticsSink.recordCardReportSaved(CardReportSavedEvent(active.cardId, reason))
                            } catch (cancellation: CancellationException) {
                                throw cancellation
                            } catch (_: Exception) {
                                // Diagnostics must not turn a persisted report into a failure.
                            }
                            mutableReportState.value = ReportUiState()
                            effectChannel.trySend(LearnSessionEffect.ReportSubmitted)
                        }

                        SubmitCardReportResult.Duplicate -> {
                            mutableReportState.value =
                                report.copy(
                                    status = ReportStatus.Error,
                                    errorMessage = "This report is already queued.",
                                )
                        }

                        SubmitCardReportResult.Unavailable -> {
                            mutableReportState.value =
                                report.copy(
                                    status = ReportStatus.Error,
                                    errorMessage = "Could not save the report. Try again.",
                                )
                        }
                    }
                }
        }

        private fun showFeedback(
            message: LearnFeedbackMessage,
            canUndo: Boolean,
            canRetry: Boolean = false,
        ) {
            feedbackJob?.cancel()
            mutableFeedbackState.value =
                LearnFeedbackUiState(
                    message = message,
                    canUndo = canUndo,
                    canRetry = canRetry,
                )
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

        private data class PendingRating(
            val sessionId: String,
            val position: Int,
            val cardId: String,
            val decision: RatingDecision,
            val source: RatingInputSource,
        )
    }

sealed interface LearnSessionEffect {
    data object SessionPaused : LearnSessionEffect

    data object PauseFailed : LearnSessionEffect

    data object ReportSubmitted : LearnSessionEffect

    data class AudioFailed(
        val reason: AudioPlaybackFailure,
    ) : LearnSessionEffect

    data class SessionCompleted(
        val completion: SessionCompletion,
    ) : LearnSessionEffect
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
                audioAssetUris = card.audioAssetUris,
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

private object NoopAudioPlayer : AudioPlayerPort {
    private val state = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)

    override fun play(request: AudioPlayRequest) = Unit

    override fun stop() {
        state.value = AudioPlaybackState.Idle
    }

    override fun observePlayback() = state.asStateFlow()
}

private object NoopUserSettingsRepository : UserSettingsRepository {
    private val state = MutableStateFlow(UserSettings())

    override val settings = state.asStateFlow()

    override suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

private fun AudioVoice.toVoiceId(): VoiceId =
    when (this) {
        AudioVoice.TYPECAST_EUNKYUNG -> VoiceId.TYPECAST_EUNKYUNG
        AudioVoice.GEMINI_KORE -> VoiceId.GEMINI_KORE
    }
