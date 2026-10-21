package com.signkorean.platform.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.signkorean.domain.model.LanguageTag
import com.signkorean.domain.port.output.AppLocalePort
import java.util.Locale

/** Uses AppCompat's Android 13 bridge and its persisted compatibility store on older releases. */
class AndroidAppLocaleAdapter(
    supportedLanguageTags: Set<String>,
) : AppLocalePort {
    private val supported = supportedLanguageTags.mapNotNull(LanguageTag::parseOrNull).map(LanguageTag::value).toSet()

    init {
        require(supported.isNotEmpty()) { "At least one app locale must be configured" }
    }

    override fun getSupported(): Set<String> = supported

    override fun getCurrent(): String {
        val applicationLocale = AppCompatDelegate.getApplicationLocales().takeIf { !it.isEmpty }
        val candidate = applicationLocale?.get(0)?.toLanguageTag() ?: Locale.getDefault().toLanguageTag()
        return supported.firstOrNull { it.equals(candidate, ignoreCase = true) }
            ?: supported.firstOrNull { languageOf(it) == languageOf(candidate) }
            ?: supported.firstOrNull { it == "en" }
            ?: supported.first()
    }

    override fun setCurrent(languageTag: String) {
        val parsed = LanguageTag(languageTag)
        require(parsed.value in supported) { "Unsupported app locale: ${parsed.value}" }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(parsed.value))
    }

    private fun languageOf(languageTag: String): String = Locale.forLanguageTag(languageTag).language.lowercase(Locale.ROOT)
}
