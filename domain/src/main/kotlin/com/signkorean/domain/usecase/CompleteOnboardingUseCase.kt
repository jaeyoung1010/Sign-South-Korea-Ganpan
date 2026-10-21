package com.signkorean.domain.usecase

import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.OnboardingCompletedEvent
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.input.CompleteOnboarding
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.UserSettingsRepository

class CompleteOnboardingUseCase(
    private val userSettingsRepository: UserSettingsRepository,
    private val analyticsSink: AnalyticsSink,
) : CompleteOnboarding {
    override suspend fun invoke(
        level: LearningLevel,
        interests: Set<Interest>,
    ): UserSettings {
        val savedSettings =
            userSettingsRepository.updateSettings {
                it.copy(
                    onboardingCompleted = true,
                    level = level,
                    interests = interests,
                )
            }
        runCatching {
            analyticsSink.recordOnboardingCompleted(
                OnboardingCompletedEvent(level = level, interestCount = interests.size),
            )
        }
        return savedSettings
    }
}
