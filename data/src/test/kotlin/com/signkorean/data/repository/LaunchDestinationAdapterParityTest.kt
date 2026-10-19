package com.signkorean.data.repository

import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.usecase.ResolveLaunchDestinationUseCase
import com.signkorean.testsupport.FakeSessionRepository
import com.signkorean.testsupport.FakeUserSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchDestinationAdapterParityTest {
    @Test
    fun `production and fake ports resolve the same onboarding destination`() =
        runTest {
            val productionDestination =
                ResolveLaunchDestinationUseCase(
                    userSettingsRepository = BootstrapUserSettingsRepository(UserSettings(onboardingCompleted = false)),
                    sessionRepository = BootstrapSessionRepository(activeSession = false),
                )()
            val fakeDestination =
                ResolveLaunchDestinationUseCase(
                    userSettingsRepository = FakeUserSettingsRepository(UserSettings(onboardingCompleted = false)),
                    sessionRepository = FakeSessionRepository(activeSession = false),
                )()

            assertEquals(LaunchDestination.ONBOARDING, productionDestination)
            assertEquals(productionDestination, fakeDestination)
        }
}
