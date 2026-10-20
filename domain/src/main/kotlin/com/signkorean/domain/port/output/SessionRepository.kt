package com.signkorean.domain.port.output

import com.signkorean.domain.model.ActiveLearnSession
import com.signkorean.domain.model.ActiveSession
import java.time.Instant

interface SessionRepository {
    suspend fun hasActiveSession(): Boolean

    suspend fun getActiveSession(): ActiveSession? = null

    suspend fun getActiveLearnSession(): ActiveLearnSession? = null

    /** Returns true only when this is the card's first persisted answer reveal. */
    suspend fun markAnswerRevealed(
        sessionId: String,
        position: Int,
        cardId: String,
        revealedAt: Instant,
    ): Boolean = false
}
