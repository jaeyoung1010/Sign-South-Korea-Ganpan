package com.signkorean.domain.port.input

sealed interface ChangeAppLanguageResult {
    data class Changed(val languageTag: String) : ChangeAppLanguageResult

    data class Unsupported(val languageTag: String) : ChangeAppLanguageResult

    data object InvalidTag : ChangeAppLanguageResult

    data object Failed : ChangeAppLanguageResult
}

interface ChangeAppLanguage {
    val supportedLanguageTags: Set<String>

    suspend operator fun invoke(languageTag: String): ChangeAppLanguageResult
}
