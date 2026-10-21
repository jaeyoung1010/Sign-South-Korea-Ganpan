package com.signkorean.domain.port.output

import com.signkorean.domain.model.AnswerRevealedEvent
import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.CardRatedEvent
import com.signkorean.domain.model.CardRatingUndoneEvent
import com.signkorean.domain.model.SessionPausedEvent
import com.signkorean.domain.model.SessionResumedEvent
import com.signkorean.domain.model.SessionStartedEvent
import com.signkorean.domain.model.SessionCompletedEvent

fun interface AnalyticsSink {
    fun recordAppStarted(event: AppStartupEvent)

    /** Records that the user chose to begin onboarding; it intentionally carries no user data. */
    fun recordOnboardingStarted() = Unit

    /** Records the completion of onboarding without attaching preference values or user data. */
    fun recordOnboardingCompleted() = Unit

    /** Records only aggregate session-selection attributes, never individual card identifiers. */
    fun recordSessionStarted(event: SessionStartedEvent) = Unit

    /** Records an answer transition only after it has been persisted. */
    fun recordAnswerRevealed(event: AnswerRevealedEvent) = Unit

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
}
