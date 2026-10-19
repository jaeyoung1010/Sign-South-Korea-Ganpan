package com.signkorean.domain.usecase

import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveLaunchDestinationUseCaseTest {
    @Test
    fun `active session resumes after onboarding is complete`() =
        runTest {
            val useCase =
                ResolveLaunchDestinationUseCase(
                    userSettingsRepository = FakeUserSettingsRepository(onboardingComplete = true),
                    sessionRepository = FakeSessionRepository(activeSession = true),
                )

            assertEquals(LaunchDestination.LEARN, useCase())
        }

    @Test
    fun `incomplete onboarding takes precedence over an active session`() =
        runTest {
            val useCase =
                ResolveLaunchDestinationUseCase(
                    userSettingsRepository = FakeUserSettingsRepository(onboardingComplete = false),
                    sessionRepository = FakeSessionRepository(activeSession = true),
                )

            assertEquals(LaunchDestination.ONBOARDING, useCase())
        }

    private class FakeUserSettingsRepository(
        private val onboardingComplete: Boolean,
    ) : UserSettingsRepository {
        override val settings: Flow<UserSettings> = flowOf(UserSettings(onboardingCompleted = onboardingComplete))

        override suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings = transform(UserSettings())
    }

    private class FakeSessionRepository(
        private val activeSession: Boolean,
    ) : SessionRepository {
        override fun hasActiveSession(): Boolean = activeSession
    }
}
