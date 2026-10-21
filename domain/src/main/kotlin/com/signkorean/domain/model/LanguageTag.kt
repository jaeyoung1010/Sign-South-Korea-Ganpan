package com.signkorean.domain.model

/** A normalized-looking BCP-47 tag whose shape is safe to pass across platform boundaries. */
data class LanguageTag(
    val value: String,
) {
    init {
        require(isValidBcp47(value)) { "Invalid BCP-47 language tag: $value" }
    }

    companion object {
        private val PRIMARY_LANGUAGE_PATTERN = Regex("^[A-Za-z]{2,8}$")
        private val SUBTAG_PATTERN = Regex("^[A-Za-z0-9]{2,8}$")
        private val PRIVATE_USE_SUBTAG_PATTERN = Regex("^[A-Za-z0-9]{1,8}$")
        private val EXTENSION_SINGLETON_PATTERN = Regex("^[0-9A-WY-Za-wy-z]$")

        fun parseOrNull(value: String): LanguageTag? = runCatching { LanguageTag(value) }.getOrNull()

        private fun isValidBcp47(value: String): Boolean {
            val subtags = value.split('-')
            if (subtags.any(String::isEmpty)) return false

            val primaryLanguage = subtags.first()
            if (primaryLanguage.equals("x", ignoreCase = true)) {
                return subtags.drop(1).isNotEmpty() && subtags.drop(1).all(PRIVATE_USE_SUBTAG_PATTERN::matches)
            }
            if (!PRIMARY_LANGUAGE_PATTERN.matches(primaryLanguage)) return false

            var index = 1
            while (index < subtags.size) {
                val subtag = subtags[index]
                if (subtag.equals("x", ignoreCase = true)) {
                    return index < subtags.lastIndex && subtags.drop(index + 1).all(PRIVATE_USE_SUBTAG_PATTERN::matches)
                }
                if (subtag.length == 1) {
                    if (!EXTENSION_SINGLETON_PATTERN.matches(subtag) || index == subtags.lastIndex) return false
                    index++
                    val extensionStart = index
                    while (index < subtags.size && subtags[index].length >= 2) index++
                    if (extensionStart == index) return false
                    continue
                }
                if (!SUBTAG_PATTERN.matches(subtag)) return false
                index++
            }
            return true
        }
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
