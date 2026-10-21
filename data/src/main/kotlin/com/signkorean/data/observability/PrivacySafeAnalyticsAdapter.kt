package com.signkorean.data.observability

import com.signkorean.domain.model.AnalyticsEvent
import com.signkorean.domain.model.AnalyticsEventName
import com.signkorean.domain.model.AnswerRevealedEvent
import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.AudioPlaybackFailedEvent
import com.signkorean.domain.model.AudioPlayedEvent
import com.signkorean.domain.model.CardReportSavedEvent
import com.signkorean.domain.model.CardRatedEvent
import com.signkorean.domain.model.CardRatingUndoneEvent
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.OnboardingCompletedEvent
import com.signkorean.domain.model.ReminderChangedEvent
import com.signkorean.domain.model.SessionCompletedEvent
import com.signkorean.domain.model.SessionPausedEvent
import com.signkorean.domain.model.SessionResumedEvent
import com.signkorean.domain.model.SessionStartedEvent
import com.signkorean.domain.model.VoiceChangedEvent
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.DiagnosticLogLevel
import com.signkorean.domain.port.output.DiagnosticLoggerPort

/** Maps typed domain events to a provider-independent, privacy-bounded event envelope. */
class PrivacySafeAnalyticsAdapter(
    private val logger: DiagnosticLoggerPort,
) : AnalyticsSink {
    override fun recordAppStarted(event: AppStartupEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.APP_OPENED.value,
                mapOf("onboarding_completed" to event.onboardingCompleted.toString()),
            ),
        )
    }

    override fun recordOnboardingStarted() {
        record(AnalyticsEvent(AnalyticsEventName.ONBOARDING_STARTED.value, emptyMap()))
    }

    override fun recordOnboardingCompleted() {
        recordOnboardingCompleted(OnboardingCompletedEvent(LearningLevel.NOT_SURE, 0))
    }

    override fun recordOnboardingCompleted(event: OnboardingCompletedEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.ONBOARDING_COMPLETED.value,
                mapOf(
                    "level" to event.level.name,
                    "interest_count" to event.interestCount.toString(),
                ),
            ),
        )
    }

    override fun recordSessionStarted(event: SessionStartedEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.SESSION_STARTED.value,
                mapOf(
                    "session_length" to (event.dueCount + event.newCount).toString(),
                    "due_count" to event.dueCount.toString(),
                    "new_count" to event.newCount.toString(),
                    "category" to (event.category ?: "all"),
                ),
            ),
        )
    }

    override fun recordAnswerRevealed(event: AnswerRevealedEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.ANSWER_REVEALED.value,
                mapOf("card_id" to event.cardId, "position" to event.position.toString()),
            ),
        )
    }

    override fun recordAudioPlaybackFailed(event: AudioPlaybackFailedEvent) {
        logger.log(
            DiagnosticLogLevel.ERROR,
            "audio_playback_failed",
            mapOf("card_id" to event.cardId, "voice_id" to event.voiceId.name, "reason" to event.reason.name),
        )
    }

    override fun recordAudioPlayed(event: AudioPlayedEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.AUDIO_PLAYED.value,
                mapOf(
                    "card_id" to event.cardId,
                    "voice_id" to event.voiceId.name,
                    "auto_play" to event.autoPlay.toString(),
                ),
            ),
        )
    }

    override fun recordVoiceChanged(event: VoiceChangedEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.VOICE_CHANGED.value,
                mapOf("previous_voice" to event.previousVoice.name, "selected_voice" to event.selectedVoice.name),
            ),
        )
    }

    override fun recordCardRated(event: CardRatedEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.CARD_RATED.value,
                mapOf(
                    "card_id" to event.cardId,
                    "rating" to event.decision.name,
                    "position" to event.position.toString(),
                    "gesture_or_accessibility" to event.source.name,
                ),
            ),
        )
    }

    override fun recordRatingUndone(event: CardRatingUndoneEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.RATING_UNDONE.value,
                mapOf("card_id" to event.cardId, "previous_rating" to event.previousRating.name),
            ),
        )
    }

    override fun recordSessionPaused(event: SessionPausedEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.SESSION_PAUSED.value,
                mapOf("position" to event.position.toString(), "session_length" to event.sessionLength.toString()),
            ),
        )
    }

    override fun recordSessionResumed(event: SessionResumedEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.SESSION_RESUMED.value,
                mapOf("position" to event.position.toString(), "session_length" to event.sessionLength.toString()),
            ),
        )
    }

    override fun recordSessionCompleted(event: SessionCompletedEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.SESSION_COMPLETED.value,
                mapOf(
                    "got_it_count" to event.gotItCount.toString(),
                    "review_count" to event.reviewCount.toString(),
                    "duration_ms" to event.durationMillis.toString(),
                ),
            ),
        )
    }

    override fun recordReminderChanged(event: ReminderChangedEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.REMINDER_CHANGED.value,
                mapOf("enabled" to event.enabled.toString()),
            ),
        )
    }

    override fun recordCardReportSaved(event: CardReportSavedEvent) {
        record(
            AnalyticsEvent(
                AnalyticsEventName.CARD_REPORT_SAVED.value,
                mapOf("card_id" to event.cardId, "reason" to event.reason.storageValue),
            ),
        )
    }

    override fun record(event: AnalyticsEvent) {
        logger.log(DiagnosticLogLevel.INFO, "analytics.${event.name}", event.properties)
    }
}
