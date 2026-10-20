package com.signkorean.domain.port.output

import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.SessionStartedEvent

fun interface AnalyticsSink {
    fun recordAppStarted(event: AppStartupEvent)

    /** Records that the user chose to begin onboarding; it intentionally carries no user data. */
    fun recordOnboardingStarted() = Unit

    /** Records the completion of onboarding without attaching preference values or user data. */
    fun recordOnboardingCompleted() = Unit

    /** Records only aggregate session-selection attributes, never individual card identifiers. */
    fun recordSessionStarted(event: SessionStartedEvent) = Unit
}
