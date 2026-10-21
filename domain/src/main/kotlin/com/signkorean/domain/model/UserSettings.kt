package com.signkorean.domain.model

import java.time.LocalTime

enum class LearningLevel {
    BEGINNER,
    ELEMENTARY,
    NOT_SURE,
}

enum class Interest {
    LANDMARK,
    NEIGHBORHOOD,
    MARKET,
    TRANSPORT,
    FOOD,
    SHOPPING,
}

enum class AudioVoice {
    TYPECAST_EUNKYUNG,
    GEMINI_KORE,
}

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val level: LearningLevel = LearningLevel.BEGINNER,
    val interests: Set<Interest> = emptySet(),
    val sessionLength: Int = DEFAULT_SESSION_LENGTH,
    val appLanguageTag: String = DEFAULT_LANGUAGE_TAG,
    val learningExplanationLanguageTag: String = DEFAULT_LANGUAGE_TAG,
    val showRomanization: Boolean = false,
    val autoPlayAudio: Boolean = false,
    val audioVoice: AudioVoice = AudioVoice.TYPECAST_EUNKYUNG,
    val reduceMotion: Boolean = false,
    val followSystemReduceMotion: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderLocalTime: LocalTime = LocalTime.of(19, 0),
) {
    init {
        require(sessionLength in SESSION_LENGTH_OPTIONS) { "Unsupported session length: $sessionLength" }
        LanguageTag(appLanguageTag)
        LanguageTag(learningExplanationLanguageTag)
    }

    companion object {
        const val DEFAULT_SESSION_LENGTH = 20
        const val DEFAULT_LANGUAGE_TAG = "en"
        val SESSION_LENGTH_OPTIONS = setOf(5, 10, 20)
    }
}
