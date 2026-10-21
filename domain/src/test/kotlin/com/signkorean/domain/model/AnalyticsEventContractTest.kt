package com.signkorean.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEventContractTest {
    @Test
    fun `all fourteen SRS events have stable names`() {
        assertEquals(14, AnalyticsEventContract.eventNames.size)
        assertEquals(
            setOf(
                "app_opened",
                "onboarding_started",
                "onboarding_completed",
                "session_started",
                "answer_revealed",
                "audio_played",
                "card_rated",
                "rating_undone",
                "session_paused",
                "session_resumed",
                "session_completed",
                "voice_changed",
                "reminder_changed",
                "card_report_saved",
            ),
            AnalyticsEventContract.eventNames,
        )
    }

    @Test
    fun `event rejects unknown names and missing required properties`() {
        org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            AnalyticsEvent("unknown", emptyMap())
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            AnalyticsEvent("audio_played", mapOf("card_id" to "card-1"))
        }
    }

    @Test
    fun `contract preserves required keys without accepting blank values`() {
        val event =
            AnalyticsEvent(
                name = "reminder_changed",
                properties = mapOf("enabled" to "false"),
            )

        assertTrue(event.properties.keys.containsAll(setOf("enabled")))
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            AnalyticsEvent("reminder_changed", mapOf("enabled" to ""))
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            AnalyticsEvent("reminder_changed", mapOf("enabled" to "false", "details" to "private"))
        }
    }
}
