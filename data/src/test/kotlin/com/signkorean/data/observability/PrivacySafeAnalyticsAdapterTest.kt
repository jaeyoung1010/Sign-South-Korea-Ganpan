package com.signkorean.data.observability

import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.AnalyticsEventName
import com.signkorean.domain.model.AnswerRevealedEvent
import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.AudioPlaybackFailedEvent
import com.signkorean.domain.model.AudioPlaybackFailure
import com.signkorean.domain.model.AudioPlayedEvent
import com.signkorean.domain.model.AudioVoice
import com.signkorean.domain.model.CardReportReason
import com.signkorean.domain.model.CardReportSavedEvent
import com.signkorean.domain.model.CardRatedEvent
import com.signkorean.domain.model.CardRatingUndoneEvent
import com.signkorean.domain.model.OnboardingCompletedEvent
import com.signkorean.domain.model.ReminderChangedEvent
import com.signkorean.domain.model.SessionCompletedEvent
import com.signkorean.domain.model.SessionPausedEvent
import com.signkorean.domain.model.SessionResumedEvent
import com.signkorean.domain.model.SessionStartedEvent
import com.signkorean.domain.model.RatingInputSource
import com.signkorean.domain.model.VoiceChangedEvent
import com.signkorean.domain.model.VoiceId
import com.signkorean.domain.port.output.DiagnosticLogLevel
import com.signkorean.domain.port.output.DiagnosticLoggerPort
import com.signkorean.domain.session.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacySafeAnalyticsAdapterTest {
    @Test
    fun `each typed event publishes exactly one contract event`() {
        val logger = RecordingLogger()
        val adapter = PrivacySafeAnalyticsAdapter(logger)

        adapter.recordAppStarted(AppStartupEvent(true, false, false, false))
        adapter.recordOnboardingStarted()
        adapter.recordOnboardingCompleted(OnboardingCompletedEvent(com.signkorean.domain.model.LearningLevel.BEGINNER, 2))
        adapter.recordSessionStarted(SessionStartedEvent(SessionType.DAILY, 2, 3, null))
        adapter.recordAnswerRevealed(AnswerRevealedEvent("card-1", 1))
        adapter.recordAudioPlayed(AudioPlayedEvent("card-1", VoiceId.GEMINI_KORE, false))
        adapter.recordCardRated(CardRatedEvent("card-1", 1, RatingDecision.GotIt, RatingInputSource.Gesture))
        adapter.recordRatingUndone(CardRatingUndoneEvent("card-1", RatingDecision.GotIt))
        adapter.recordSessionPaused(SessionPausedEvent(1, 5))
        adapter.recordSessionResumed(SessionResumedEvent(1, 5))
        adapter.recordSessionCompleted(SessionCompletedEvent(3, 2, 1_000))
        adapter.recordVoiceChanged(VoiceChangedEvent(AudioVoice.TYPECAST_EUNKYUNG, AudioVoice.GEMINI_KORE))
        adapter.recordReminderChanged(ReminderChangedEvent(true))
        adapter.recordCardReportSaved(CardReportSavedEvent("card-1", CardReportReason.WRONG_MEANING))

        assertEquals(14, logger.entries.size)
        assertEquals(
            AnalyticsEventName.entries.map(AnalyticsEventName::value).toSet(),
            logger.entries.map { it.message.removePrefix("analytics.") }.toSet(),
        )
    }

    @Test
    fun `repeated calls are not duplicated by the adapter`() {
        val logger = RecordingLogger()
        val adapter = PrivacySafeAnalyticsAdapter(logger)
        val event = AudioPlayedEvent("card-1", VoiceId.TYPECAST_EUNKYUNG, true)

        adapter.recordAudioPlayed(event)
        adapter.recordAudioPlayed(event)

        assertEquals(2, logger.entries.count { it.message == "analytics.audio_played" })
    }

    @Test
    fun `playback failure is diagnostic only and does not expose an asset path`() {
        val logger = RecordingLogger()
        val adapter = PrivacySafeAnalyticsAdapter(logger)

        adapter.recordAudioPlaybackFailed(AudioPlaybackFailedEvent("card-1", VoiceId.GEMINI_KORE, AudioPlaybackFailure.Decode))

        assertEquals(1, logger.entries.size)
        assertEquals(DiagnosticLogLevel.ERROR, logger.entries.single().level)
        assertEquals("audio_playback_failed", logger.entries.single().message)
        assertFalse(logger.entries.single().attributes.values.any { it.contains("/", ignoreCase = false) })
    }

    @Test
    fun `diagnostic logger redacts free input paths keys and identifiers`() {
        val delegate = RecordingLogger()
        val logger = RedactingDiagnosticLogger(delegate)

        logger.log(
            DiagnosticLogLevel.ERROR,
            "details=wrong private street address path=/Users/choi/report.txt api_key=tts-secret user@example.com",
            mapOf(
                "details" to "full report text",
                "path" to "/private/data/report.txt",
                "api_key" to "tts-secret",
                "user_id" to "user-123",
                "free_text" to "untrusted input",
                "card_id" to "card-1",
            ),
        )

        val entry = delegate.entries.single()
        val rendered = entry.message + entry.attributes
        assertFalse(rendered.contains("wrong private street address"))
        assertFalse(rendered.contains("/Users/choi/report.txt"))
        assertFalse(rendered.contains("tts-secret"))
        assertFalse(rendered.contains("user@example.com"))
        assertFalse(rendered.contains("user-123"))
        assertFalse(rendered.contains("untrusted input"))
        assertTrue(entry.attributes["card_id"] == "card-1")
    }

    private data class Entry(
        val level: DiagnosticLogLevel,
        val message: String,
        val attributes: Map<String, String>,
    )

    private class RecordingLogger : DiagnosticLoggerPort {
        val entries = mutableListOf<Entry>()

        override fun log(level: DiagnosticLogLevel, message: String, attributes: Map<String, String>) {
            entries += Entry(level, message, attributes)
        }
    }
}
