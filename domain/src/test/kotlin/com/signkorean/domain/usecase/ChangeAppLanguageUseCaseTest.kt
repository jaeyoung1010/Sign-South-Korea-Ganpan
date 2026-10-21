package com.signkorean.domain.usecase

import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.input.ChangeAppLanguageResult
import com.signkorean.domain.port.output.AppLocalePort
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChangeAppLanguageUseCaseTest {
    @Test
    fun `changes UI and explanation languages together while keeping the concepts separate`() =
        runTest {
            val settings = FakeSettingsRepository(UserSettings())
            val locale = FakeAppLocalePort(setOf("en", "ja"))

            val result = ChangeAppLanguageUseCase(locale, settings)("ja")

            assertEquals(ChangeAppLanguageResult.Changed("ja"), result)
            assertEquals("ja", settings.settings.first().appLanguageTag)
            assertEquals("ja", settings.settings.first().learningExplanationLanguageTag)
            assertEquals("ja", locale.currentTag)
        }

    @Test
    fun `rejects malformed and unsupported language tags without changing settings`() =
        runTest {
            val settings = FakeSettingsRepository(UserSettings())
            val locale = FakeAppLocalePort(setOf("en", "ja"))
            val useCase = ChangeAppLanguageUseCase(locale, settings)

            assertEquals(ChangeAppLanguageResult.InvalidTag, useCase("en_US"))
            assertEquals(ChangeAppLanguageResult.Unsupported("fr"), useCase("fr"))
            assertEquals("en", settings.settings.first().appLanguageTag)
            assertEquals("en", locale.currentTag)
        }

    private class FakeAppLocalePort(
        private val supportedLanguageTags: Set<String>,
        var currentTag: String = "en",
    ) : AppLocalePort {
        override fun getSupported(): Set<String> = supportedLanguageTags

        override fun getCurrent(): String = currentTag

        override fun setCurrent(languageTag: String) {
            currentTag = languageTag
        }
    }

    private class FakeSettingsRepository(initial: UserSettings) : UserSettingsRepository {
        private val state = MutableStateFlow(initial)
        override val settings: Flow<UserSettings> = state

        override suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings =
            transform(state.value).also { state.value = it }
    }
}
