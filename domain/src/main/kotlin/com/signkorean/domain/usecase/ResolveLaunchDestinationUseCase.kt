package com.signkorean.domain.usecase

import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.port.input.ResolveLaunchDestination
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository

class ResolveLaunchDestinationUseCase(
    private val userSettingsRepository: UserSettingsRepository,
    private val sessionRepository: SessionRepository,
) : ResolveLaunchDestination {
    override fun invoke(): LaunchDestination =
        when {
            !userSettingsRepository.isOnboardingComplete() -> LaunchDestination.ONBOARDING
            sessionRepository.hasActiveSession() -> LaunchDestination.LEARN
            else -> LaunchDestination.HOME
        }
}
