package com.signkorean.domain.port.output

interface UserSettingsRepository {
    fun isOnboardingComplete(): Boolean
}
