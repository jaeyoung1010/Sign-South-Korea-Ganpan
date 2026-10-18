package com.signkorean.data.repository

import com.signkorean.domain.port.output.UserSettingsRepository

class BootstrapUserSettingsRepository(
    private val onboardingComplete: Boolean = false,
) : UserSettingsRepository {
    override fun isOnboardingComplete(): Boolean = onboardingComplete
}
