package com.signkorean.domain.port.output

import com.signkorean.domain.model.AnalyticsEvent
import com.signkorean.domain.model.AnswerRevealedEvent
import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.AudioPlaybackFailedEvent
import com.signkorean.domain.model.AudioPlayedEvent
import com.signkorean.domain.model.CardRatedEvent
import com.signkorean.domain.model.CardRatingUndoneEvent
import com.signkorean.domain.model.CardReportSavedEvent
import com.signkorean.domain.model.OnboardingCompletedEvent
import com.signkorean.domain.model.ReminderChangedEvent
import com.signkorean.domain.model.SessionCompletedEvent
import com.signkorean.domain.model.SessionPausedEvent
import com.signkorean.domain.model.SessionResumedEvent
import com.signkorean.domain.model.SessionStartedEvent
import com.signkorean.domain.model.VoiceChangedEvent

interface AnalyticsPort {
    fun record(event: AnalyticsEvent) = Unit
}

fun interface AnalyticsSink : AnalyticsPort {
    fun recordAppStarted(event: AppStartupEvent)

    /** Records that the user chose to begin onboarding; it intentionally carries no user data. */
    fun recordOnboardingStarted() = Unit

    /** Records the completion of onboarding without attaching preference values or user data. */
    fun recordOnboardingCompleted() = Unit

    fun recordOnboardingCompleted(event: OnboardingCompletedEvent) {
        recordOnboardingCompleted()
    }

    /** Records only aggregate session-selection attributes, never individual card identifiers. */
    fun recordSessionStarted(event: SessionStartedEvent) = Unit

    /** Records an answer transition only after it has been persisted. */
    fun recordAnswerRevealed(event: AnswerRevealedEvent) = Unit

    /** Records only typed audio failure context; asset paths and user content are excluded. */
    fun recordAudioPlaybackFailed(event: AudioPlaybackFailedEvent) = Unit

    /** Records a playback request without retaining its asset path or spoken content. */
    fun recordAudioPlayed(event: AudioPlayedEvent) = Unit

    /** Records only the old and new bundled voice identifiers. */
    fun recordVoiceChanged(event: VoiceChangedEvent) = Unit

    /** Records a persisted rating and the input path that produced it. */
    fun recordCardRated(event: CardRatedEvent) = Unit

    /** Records that the latest persisted rating was rolled back. */
    fun recordRatingUndone(event: CardRatingUndoneEvent) = Unit

    /** Records a pause without attaching card identifiers or user data. */
    fun recordSessionPaused(event: SessionPausedEvent) = Unit

    /** Records a resume without attaching card identifiers or user data. */
    fun recordSessionResumed(event: SessionResumedEvent) = Unit

    /** Records only aggregate completion results and elapsed time. */
    fun recordSessionCompleted(event: SessionCompletedEvent) = Unit

    fun recordReminderChanged(event: ReminderChangedEvent) = Unit

    fun recordCardReportSaved(event: CardReportSavedEvent) = Unit
}
