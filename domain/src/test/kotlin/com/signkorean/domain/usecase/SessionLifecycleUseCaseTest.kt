package com.signkorean.domain.usecase

import com.signkorean.domain.model.ActiveSession
import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.SessionPausedEvent
import com.signkorean.domain.model.SessionResumedEvent
import com.signkorean.domain.model.SessionTransitionWriteResult
import com.signkorean.domain.port.input.PauseSessionRequest
import com.signkorean.domain.port.input.PauseSessionResult
import com.signkorean.domain.port.input.ResumeSessionRequest
import com.signkorean.domain.port.input.ResumeSessionResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.SessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLifecycleUseCaseTest {
    @Test
    fun `pause changes an active session and emits aggregate event`() =
        runTest {
            val analytics = RecordingAnalytics()
            val result =
                PauseSessionUseCase(
                    FakeSessionRepository(SessionTransitionWriteResult.Changed(ActiveSession(2, 5, "session-1", isPaused = true))),
                    analytics,
                )(PauseSessionRequest("session-1"))

            assertEquals(PauseSessionResult.Paused(ActiveSession(2, 5, "session-1", isPaused = true)), result)
            assertEquals(listOf(SessionPausedEvent(2, 5)), analytics.paused)
        }

    @Test
    fun `pause is idempotent and does not emit a duplicate event`() =
        runTest {
            val analytics = RecordingAnalytics()
            val result =
                PauseSessionUseCase(
                    FakeSessionRepository(SessionTransitionWriteResult.AlreadyInState(ActiveSession(2, 5, "session-1", isPaused = true))),
                    analytics,
                )(PauseSessionRequest("session-1"))

            assertEquals(PauseSessionResult.AlreadyPaused(ActiveSession(2, 5, "session-1", isPaused = true)), result)
            assertTrue(analytics.paused.isEmpty())
        }

    @Test
    fun `resume changes a paused session and emits aggregate event`() =
        runTest {
            val analytics = RecordingAnalytics()
            val result =
                ResumeSessionUseCase(
                    FakeSessionRepository(SessionTransitionWriteResult.Changed(ActiveSession(2, 5, "session-1"))),
                    analytics,
                )(ResumeSessionRequest("session-1"))

            assertEquals(ResumeSessionResult.Resumed(ActiveSession(2, 5, "session-1")), result)
            assertEquals(listOf(SessionResumedEvent(2, 5)), analytics.resumed)
        }

    @Test
    fun `repository failure becomes unavailable without analytics`() =
        runTest {
            val analytics = RecordingAnalytics()
            val result =
                ResumeSessionUseCase(
                    object : SessionRepository {
                        override suspend fun hasActiveSession(): Boolean = false

                        override suspend fun resumeSession(sessionId: String): SessionTransitionWriteResult = error("database unavailable")
                    },
                    analytics,
                )(ResumeSessionRequest("session-1"))

            assertEquals(ResumeSessionResult.Unavailable, result)
            assertTrue(analytics.resumed.isEmpty())
        }

    private class FakeSessionRepository(
        private val transition: SessionTransitionWriteResult,
    ) : SessionRepository {
        override suspend fun hasActiveSession(): Boolean = true

        override suspend fun pauseSession(sessionId: String): SessionTransitionWriteResult = transition

        override suspend fun resumeSession(sessionId: String): SessionTransitionWriteResult = transition
    }

    private class RecordingAnalytics : AnalyticsSink {
        val paused = mutableListOf<SessionPausedEvent>()
        val resumed = mutableListOf<SessionResumedEvent>()

        override fun recordAppStarted(event: AppStartupEvent) = Unit

        override fun recordSessionPaused(event: SessionPausedEvent) {
            paused += event
        }

        override fun recordSessionResumed(event: SessionResumedEvent) {
            resumed += event
        }
    }
}
