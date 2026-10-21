package com.signkorean.domain.usecase

import com.signkorean.domain.model.LanguageTag
import com.signkorean.domain.port.input.ChangeAppLanguage
import com.signkorean.domain.port.input.ChangeAppLanguageResult
import com.signkorean.domain.port.output.AppLocalePort
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.flow.first

class ChangeAppLanguageUseCase(
    private val appLocalePort: AppLocalePort,
    private val userSettingsRepository: UserSettingsRepository,
) : ChangeAppLanguage {
    override val supportedLanguageTags: Set<String>
        get() = appLocalePort.getSupported()

    override suspend fun invoke(languageTag: String): ChangeAppLanguageResult {
        val parsed = LanguageTag.parseOrNull(languageTag) ?: return ChangeAppLanguageResult.InvalidTag
        if (parsed.value !in supportedLanguageTags) return ChangeAppLanguageResult.Unsupported(parsed.value)

        val previous = userSettingsRepository.settings.first()
        return try {
            userSettingsRepository.updateSettings { settings ->
                settings.copy(
                    appLanguageTag = parsed.value,
                    // The first release keeps UI and explanation language in sync while preserving
                    // separate fields for a future mixed-language learning mode.
                    learningExplanationLanguageTag = parsed.value,
                )
            }
            appLocalePort.setCurrent(parsed.value)
            ChangeAppLanguageResult.Changed(parsed.value)
        } catch (_: Exception) {
            runCatching {
                userSettingsRepository.updateSettings { settings ->
                    settings.copy(
                        appLanguageTag = previous.appLanguageTag,
                        learningExplanationLanguageTag = previous.learningExplanationLanguageTag,
                    )
                }
            }
            ChangeAppLanguageResult.Failed
        }
    }
}
