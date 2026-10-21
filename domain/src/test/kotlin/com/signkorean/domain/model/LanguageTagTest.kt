package com.signkorean.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LanguageTagTest {
    @Test
    fun `accepts BCP 47 language tags including regional and script subtags`() {
        assertEquals("en", LanguageTag("en").value)
        assertEquals("ja-JP", LanguageTag("ja-JP").value)
        assertEquals("zh-Hant-TW", LanguageTag("zh-Hant-TW").value)
    }

    @Test
    fun `rejects blank malformed and underscore separated tags`() {
        listOf("", " ", "e", "en_US", "en-", "-US").forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { LanguageTag(value) }
        }
    }

    @Test
    fun `UI language and learning explanation language remain distinct concepts`() {
        val uiLanguage = UiLanguage(LanguageTag("en"))
        val explanationLanguage = LearningExplanationLanguage(LanguageTag("ja"))

        assertEquals("en", uiLanguage.tag.value)
        assertEquals("ja", explanationLanguage.tag.value)
        assertNotEquals(uiLanguage.tag, explanationLanguage.tag)
    }
}
