package com.signkorean.domain.usecase

import com.signkorean.domain.model.SessionResumedEvent
import com.signkorean.domain.model.SessionTransitionWriteResult
import com.signkorean.domain.port.input.ResumeSession
import com.signkorean.domain.port.input.ResumeSessionRequest
import com.signkorean.domain.port.input.ResumeSessionResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.SessionRepository
import kotlinx.coroutines.CancellationException

class ResumeSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val analyticsSink: AnalyticsSink,
) : ResumeSession {
    override suspend fun invoke(request: ResumeSessionRequest): ResumeSessionResult {
        val result =
            try {
                sessionRepository.resumeSession(request.sessionId).toResult()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                ResumeSessionResult.Unavailable
            }
        if (result is ResumeSessionResult.Resumed) {
            try {
                analyticsSink.recordSessionResumed(
                    SessionResumedEvent(
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

    private fun SessionTransitionWriteResult.toResult(): ResumeSessionResult =
        when (this) {
            is SessionTransitionWriteResult.Changed -> ResumeSessionResult.Resumed(session)
            is SessionTransitionWriteResult.AlreadyInState -> ResumeSessionResult.AlreadyActive(session)
            SessionTransitionWriteResult.Unavailable -> ResumeSessionResult.Unavailable
        }
}
