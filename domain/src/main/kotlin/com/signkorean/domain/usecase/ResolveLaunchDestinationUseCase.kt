package com.signkorean.domain.usecase

import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.port.input.ResolveLaunchDestination
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.flow.first

class ResolveLaunchDestinationUseCase(
    private val userSettingsRepository: UserSettingsRepository,
    private val sessionRepository: SessionRepository,
) : ResolveLaunchDestination {
    override suspend fun invoke(): LaunchDestination =
        when {
            !userSettingsRepository.settings.first().onboardingCompleted -> LaunchDestination.ONBOARDING
            sessionRepository.hasActiveSession() -> LaunchDestination.LEARN
            else -> LaunchDestination.HOME
        }
}
