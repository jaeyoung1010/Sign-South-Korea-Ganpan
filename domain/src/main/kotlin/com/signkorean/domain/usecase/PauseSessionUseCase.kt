package com.signkorean.domain.usecase

import com.signkorean.domain.model.SessionPausedEvent
import com.signkorean.domain.model.SessionTransitionWriteResult
import com.signkorean.domain.port.input.PauseSession
import com.signkorean.domain.port.input.PauseSessionRequest
import com.signkorean.domain.port.input.PauseSessionResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.SessionRepository
import kotlinx.coroutines.CancellationException

class PauseSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val analyticsSink: AnalyticsSink,
) : PauseSession {
    override suspend fun invoke(request: PauseSessionRequest): PauseSessionResult {
        val result =
            try {
                sessionRepository.pauseSession(request.sessionId).toResult()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                PauseSessionResult.Unavailable
            }
        if (result is PauseSessionResult.Paused) {
            try {
                analyticsSink.recordSessionPaused(
                    SessionPausedEvent(
                        position = result.session.completedCount,
                        sessionLength = result.session.totalCount,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
            }
        }
        return result
    }

    private fun SessionTransitionWriteResult.toResult(): PauseSessionResult =
        when (this) {
            is SessionTransitionWriteResult.Changed -> PauseSessionResult.Paused(session)
            is SessionTransitionWriteResult.AlreadyInState -> PauseSessionResult.AlreadyPaused(session)
            SessionTransitionWriteResult.Unavailable -> PauseSessionResult.Unavailable
        }
}
