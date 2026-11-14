package com.signkorean.domain.port.output

import com.signkorean.domain.model.ActiveLearnSession
import com.signkorean.domain.model.ActiveSession
import com.signkorean.domain.model.AnswerRevealWriteResult
import com.signkorean.domain.model.SessionCompletion
import com.signkorean.domain.model.SessionTransitionWriteResult
import java.time.Instant

interface SessionRepository {
    suspend fun hasActiveSession(): Boolean

    suspend fun getActiveSession(): ActiveSession? = null

    suspend fun getActiveLearnSession(): ActiveLearnSession? = null

    suspend fun getSessionCompletion(sessionId: String): SessionCompletion? = null

    /** Reports whether the reveal was persisted, had already been persisted, or is stale. */
    suspend fun markAnswerRevealed(
        sessionId: String,
        position: Int,
        cardId: String,
        revealedAt: Instant,
    ): AnswerRevealWriteResult = AnswerRevealWriteResult.Unavailable

    suspend fun pauseSession(sessionId: String): SessionTransitionWriteResult = SessionTransitionWriteResult.Unavailable

    suspend fun resumeSession(sessionId: String): SessionTransitionWriteResult = SessionTransitionWriteResult.Unavailable
}
