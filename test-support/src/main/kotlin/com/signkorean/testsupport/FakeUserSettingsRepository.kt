package com.signkorean.testsupport

import com.signkorean.domain.port.output.UserSettingsRepository

class FakeUserSettingsRepository(
    var onboardingComplete: Boolean = false,
) : UserSettingsRepository {
    override fun isOnboardingComplete(): Boolean = onboardingComplete
}
