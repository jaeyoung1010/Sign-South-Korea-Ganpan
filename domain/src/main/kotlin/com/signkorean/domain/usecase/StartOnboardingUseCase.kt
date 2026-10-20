package com.signkorean.domain.usecase

import com.signkorean.domain.port.input.StartOnboarding
import com.signkorean.domain.port.output.AnalyticsSink

class StartOnboardingUseCase(
    private val analyticsSink: AnalyticsSink,
) : StartOnboarding {
    override fun invoke() {
        analyticsSink.recordOnboardingStarted()
    }
}
