package com.signkorean.domain.usecase

import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.port.output.AnalyticsSink
import org.junit.Assert.assertEquals
import org.junit.Test

class StartOnboardingUseCaseTest {
    @Test
    fun `records onboarding started without any user payload`() {
        val analytics = RecordingAnalyticsSink()

        StartOnboardingUseCase(analytics)()

        assertEquals(1, analytics.onboardingStartedCount)
    }

    private class RecordingAnalyticsSink : AnalyticsSink {
        var onboardingStartedCount = 0

        override fun recordAppStarted(event: AppStartupEvent) = Unit

        override fun recordOnboardingStarted() {
            onboardingStartedCount += 1
        }
    }
}
