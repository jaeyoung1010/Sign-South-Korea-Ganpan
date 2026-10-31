package com.signkorean.domain.usecase

import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.OnboardingCompletedEvent
import com.signkorean.domain.model.ProfileAvatar
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.input.CompleteOnboarding
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.AppLocalePort
import com.signkorean.domain.port.output.UserSettingsRepository

class CompleteOnboardingUseCase(
    private val userSettingsRepository: UserSettingsRepository,
    private val analyticsSink: AnalyticsSink,
    private val appLocalePort: AppLocalePort = NoopAppLocalePort,
) : CompleteOnboarding {
    override suspend fun invoke(
        level: LearningLevel,
        interests: Set<Interest>,
        profileName: String,
        profileAvatar: ProfileAvatar,
    ): UserSettings {
        val normalizedName = profileName.trim().take(UserSettings.MAX_PROFILE_NAME_LENGTH)
        require(normalizedName.isNotBlank()) { "Profile name is required" }
        val savedSettings =
            userSettingsRepository.updateSettings {
                it.copy(
                    onboardingCompleted = true,
                    level = level,
                    interests = interests,
                    profileName = normalizedName,
                    profileAvatar = profileAvatar,
                    appLanguageTag = profileAvatar.languageTag,
                    learningExplanationLanguageTag = profileAvatar.languageTag,
                )
            }
        appLocalePort.setCurrent(profileAvatar.languageTag)
        runCatching {
            analyticsSink.recordOnboardingCompleted(
                OnboardingCompletedEvent(level = level, interestCount = interests.size),
            )
        }
        return savedSettings
    }
}

private object NoopAppLocalePort : AppLocalePort {
    override fun getSupported(): Set<String> = setOf("en", "ja", "zh")

    override fun getCurrent(): String = "en"

    override fun setCurrent(languageTag: String) = Unit
}
