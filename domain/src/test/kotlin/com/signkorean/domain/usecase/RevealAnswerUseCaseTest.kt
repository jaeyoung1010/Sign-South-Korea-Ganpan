package com.signkorean.domain.usecase

import com.signkorean.domain.model.ActiveLearnSession
import com.signkorean.domain.model.AnswerRevealedEvent
import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.LearnSessionCard
import com.signkorean.domain.port.input.RevealAnswerResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.SessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class RevealAnswerUseCaseTest {
    @Test
    fun `persists and records an answer reveal exactly once`() =
        runTest {
            val sessions = RecordingSessionRepository(markResult = true)
            val analytics = RecordingAnalyticsSink()
            val useCase = RevealAnswerUseCase(sessions, analytics, fixedClock)

            val result = useCase()

            assertEquals(RevealAnswerResult.Revealed, result)
            assertEquals(listOf(Triple("session-1", 1, "card-2")), sessions.recorded)
            assertEquals(listOf(AnswerRevealedEvent("card-2", 2)), analytics.events)
        }

    @Test
    fun `does not duplicate analytics when the answer was already revealed`() =
        runTest {
            val sessions = RecordingSessionRepository(markResult = false)
            val analytics = RecordingAnalyticsSink()

            val result = RevealAnswerUseCase(sessions, analytics, fixedClock)()

            assertEquals(RevealAnswerResult.AlreadyRevealed, result)
            assertEquals(emptyList<AnswerRevealedEvent>(), analytics.events)
        }

    private class RecordingSessionRepository(
        private val markResult: Boolean,
    ) : SessionRepository {
        val recorded = mutableListOf<Triple<String, Int, String>>()

        override suspend fun hasActiveSession(): Boolean = true

        override suspend fun getActiveLearnSession() =
            ActiveLearnSession(
                sessionId = "session-1",
                currentIndex = 1,
                requestedLength = 3,
                cards = listOf(LearnSessionCard(0, "card-1"), LearnSessionCard(1, "card-2"), LearnSessionCard(2, "card-3")),
            )

        override suspend fun markAnswerRevealed(
            sessionId: String,
            position: Int,
            cardId: String,
            revealedAt: Instant,
        ): Boolean {
            recorded += Triple(sessionId, position, cardId)
            return markResult
        }
    }

    private class RecordingAnalyticsSink : AnalyticsSink {
        val events = mutableListOf<AnswerRevealedEvent>()

        override fun recordAppStarted(event: AppStartupEvent) = Unit

        override fun recordAnswerRevealed(event: AnswerRevealedEvent) {
            events += event
        }
    }

    private companion object {
        val fixedClock =
            object : ClockPort {
                override fun now(): Instant = Instant.parse("2026-10-20T00:00:00Z")

                override fun zoneId(): ZoneId = ZoneId.of("UTC")
            }
    }
}
