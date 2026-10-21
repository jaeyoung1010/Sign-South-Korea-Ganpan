package com.signkorean.domain.model

enum class AnalyticsEventName(
    val value: String,
) {
    APP_OPENED("app_opened"),
    ONBOARDING_STARTED("onboarding_started"),
    ONBOARDING_COMPLETED("onboarding_completed"),
    SESSION_STARTED("session_started"),
    ANSWER_REVEALED("answer_revealed"),
    AUDIO_PLAYED("audio_played"),
    CARD_RATED("card_rated"),
    RATING_UNDONE("rating_undone"),
    SESSION_PAUSED("session_paused"),
    SESSION_RESUMED("session_resumed"),
    SESSION_COMPLETED("session_completed"),
    VOICE_CHANGED("voice_changed"),
    REMINDER_CHANGED("reminder_changed"),
    CARD_REPORT_SAVED("card_report_saved"),
}

object AnalyticsEventContract {
    val requiredAttributes: Map<AnalyticsEventName, Set<String>> =
        mapOf(
            AnalyticsEventName.APP_OPENED to setOf("onboarding_completed"),
            AnalyticsEventName.ONBOARDING_STARTED to emptySet(),
            AnalyticsEventName.ONBOARDING_COMPLETED to setOf("level", "interest_count"),
            AnalyticsEventName.SESSION_STARTED to setOf("session_length", "due_count", "new_count", "category"),
            AnalyticsEventName.ANSWER_REVEALED to setOf("card_id", "position"),
            AnalyticsEventName.AUDIO_PLAYED to setOf("card_id", "voice_id", "auto_play"),
            AnalyticsEventName.CARD_RATED to setOf("card_id", "rating", "position", "gesture_or_accessibility"),
            AnalyticsEventName.RATING_UNDONE to setOf("card_id", "previous_rating"),
            AnalyticsEventName.SESSION_PAUSED to setOf("position", "session_length"),
            AnalyticsEventName.SESSION_RESUMED to setOf("position", "session_length"),
            AnalyticsEventName.SESSION_COMPLETED to setOf("got_it_count", "review_count", "duration_ms"),
            AnalyticsEventName.VOICE_CHANGED to setOf("previous_voice", "selected_voice"),
            AnalyticsEventName.REMINDER_CHANGED to setOf("enabled"),
            AnalyticsEventName.CARD_REPORT_SAVED to setOf("card_id", "reason"),
        )

    val eventNames: Set<String> = requiredAttributes.keys.mapTo(linkedSetOf(), AnalyticsEventName::value)
}

data class AnalyticsEvent(
    val name: String,
    val properties: Map<String, String>,
) {
    init {
        val eventName = AnalyticsEventName.entries.firstOrNull { it.value == name }
            ?: error("Unknown analytics event: $name")
        val requiredAttributes = AnalyticsEventContract.requiredAttributes.getValue(eventName)
        require(properties.keys == requiredAttributes) {
            "Analytics properties must match the contract for $name"
        }
        require(properties.values.all(String::isNotBlank)) { "Analytics properties must not be blank" }
    }
}

data class OnboardingCompletedEvent(
    val level: LearningLevel,
    val interestCount: Int,
) {
    init {
        require(interestCount >= 0)
    }
}

data class ReminderChangedEvent(
    val enabled: Boolean,
)

data class CardReportSavedEvent(
    val cardId: String,
    val reason: CardReportReason,
) {
    init {
        require(cardId.isNotBlank())
    }
}
