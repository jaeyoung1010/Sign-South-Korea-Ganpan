package com.signkorean.data.repository

import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.usecase.ResolveLaunchDestinationUseCase
import com.signkorean.testsupport.FakeSessionRepository
import com.signkorean.testsupport.FakeUserSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchDestinationAdapterParityTest {
    @Test
    fun `production and fake ports resolve the same onboarding destination`() {
        val productionDestination =
            ResolveLaunchDestinationUseCase(
                userSettingsRepository = BootstrapUserSettingsRepository(onboardingComplete = false),
                sessionRepository = BootstrapSessionRepository(activeSession = false),
            )()
        val fakeDestination =
            ResolveLaunchDestinationUseCase(
                userSettingsRepository = FakeUserSettingsRepository(onboardingComplete = false),
                sessionRepository = FakeSessionRepository(activeSession = false),
            )()

        assertEquals(LaunchDestination.ONBOARDING, productionDestination)
        assertEquals(productionDestination, fakeDestination)
    }
}
