package com.signkorean.domain.model

/** A normalized-looking BCP-47 tag whose shape is safe to pass across platform boundaries. */
data class LanguageTag(
    val value: String,
) {
    init {
        require(BCP_47_PATTERN.matches(value)) { "Invalid BCP-47 language tag: $value" }
    }

    companion object {
        private val BCP_47_PATTERN = Regex("^[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*$")

        fun parseOrNull(value: String): LanguageTag? = runCatching { LanguageTag(value) }.getOrNull()
    }
}

@JvmInline
value class UiLanguage(
    val tag: LanguageTag,
)

@JvmInline
value class LearningExplanationLanguage(
    val tag: LanguageTag,
)
