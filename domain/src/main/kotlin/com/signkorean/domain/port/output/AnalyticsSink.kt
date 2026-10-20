package com.signkorean.domain.port.output

import com.signkorean.domain.model.AppStartupEvent

fun interface AnalyticsSink {
    fun recordAppStarted(event: AppStartupEvent)

    /** Records that the user chose to begin onboarding; it intentionally carries no user data. */
    fun recordOnboardingStarted() = Unit
}
