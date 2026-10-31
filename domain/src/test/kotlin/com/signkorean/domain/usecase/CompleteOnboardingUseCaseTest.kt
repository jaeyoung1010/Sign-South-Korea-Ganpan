package com.signkorean.domain.usecase

import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.OnboardingCompletedEvent
import com.signkorean.domain.model.ProfileAvatar
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.AppLocalePort
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CompleteOnboardingUseCaseTest {
    @Test
    fun `persists selected level and interests then records completion`() =
        runBlocking {
            val settings = RecordingSettingsRepository()
            val analytics = RecordingAnalyticsSink()
            val locale = RecordingAppLocalePort()

            CompleteOnboardingUseCase(settings, analytics, locale)(
                level = LearningLevel.ELEMENTARY,
                interests = setOf(Interest.FOOD, Interest.TRANSPORT),
                profileName = "Mina",
                profileAvatar = ProfileAvatar.JAPANESE_WOMAN,
            )

            assertEquals(
                UserSettings(
                    onboardingCompleted = true,
                    level = LearningLevel.ELEMENTARY,
                    interests = setOf(Interest.FOOD, Interest.TRANSPORT),
                    profileName = "Mina",
                    profileAvatar = ProfileAvatar.JAPANESE_WOMAN,
                    appLanguageTag = "ja",
                    learningExplanationLanguageTag = "ja",
                ),
                settings.value,
            )
            assertEquals(OnboardingCompletedEvent(LearningLevel.ELEMENTARY, 2), analytics.completedEvent)
            assertEquals("ja", locale.currentTag)
        }

    private class RecordingSettingsRepository : UserSettingsRepository {
        private val mutableSettings = MutableStateFlow(UserSettings())

        val value: UserSettings
            get() = mutableSettings.value

        override val settings: Flow<UserSettings> = mutableSettings

        override suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings =
            transform(mutableSettings.value).also { mutableSettings.value = it }
    }

    private class RecordingAnalyticsSink : AnalyticsSink {
        var completedEvent: OnboardingCompletedEvent? = null

        override fun recordAppStarted(event: AppStartupEvent) = Unit

        override fun recordOnboardingCompleted(event: OnboardingCompletedEvent) {
            completedEvent = event
        }
    }

    private class RecordingAppLocalePort : AppLocalePort {
        var currentTag = "en"

        override fun getSupported(): Set<String> = setOf("en", "ja", "zh")

        override fun getCurrent(): String = currentTag

        override fun setCurrent(languageTag: String) {
            currentTag = languageTag
        }
    }
}
