package com.signkorean.data.repository

import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.model.ActiveLearnSession
import com.signkorean.domain.model.ActiveSession
import com.signkorean.domain.model.AnswerRevealWriteResult
import com.signkorean.domain.model.LearnSessionCard
import com.signkorean.domain.port.output.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant

class RoomSessionRepository(
    private val database: SignKoreanDatabase,
    private val dispatcher: CoroutineDispatcher,
) : SessionRepository {
    override suspend fun hasActiveSession(): Boolean =
        withContext(dispatcher) {
            database.sessionDao().active() != null
        }

    override suspend fun getActiveSession(): ActiveSession? =
        withContext(dispatcher) {
            database.sessionDao().active()?.let { session ->
                ActiveSession(completedCount = session.currentIndex, totalCount = session.requestedLength)
            }
        }

    override suspend fun getActiveLearnSession(): ActiveLearnSession? =
        withContext(dispatcher) {
            database.sessionDao().active()?.let { session ->
                ActiveLearnSession(
                    sessionId = session.sessionId,
                    currentIndex = session.currentIndex,
                    requestedLength = session.requestedLength,
                    cards =
                        database.sessionCardDao().forSession(session.sessionId).map { card ->
                            LearnSessionCard(
                                position = card.position,
                                cardId = card.cardId,
                                answerRevealedAt = card.answerRevealedAt,
                            )
                        },
                )
            }
        }

    override suspend fun markAnswerRevealed(
        sessionId: String,
        position: Int,
        cardId: String,
        revealedAt: Instant,
    ): AnswerRevealWriteResult =
        withContext(dispatcher) {
            database.runInTransaction<AnswerRevealWriteResult> {
                val session = database.sessionDao().get(sessionId) ?: return@runInTransaction AnswerRevealWriteResult.Unavailable
                if (session.status != "ACTIVE" || session.currentIndex != position) {
                    return@runInTransaction AnswerRevealWriteResult.Unavailable
                }
                val card =
                    database.sessionCardDao().get(sessionId, position)
                        ?: return@runInTransaction AnswerRevealWriteResult.Unavailable
                if (card.cardId != cardId) return@runInTransaction AnswerRevealWriteResult.Unavailable
                if (card.answerRevealedAt != null) return@runInTransaction AnswerRevealWriteResult.AlreadyRecorded
                val updated = database.sessionCardDao().markAnswerRevealed(sessionId, position, cardId, revealedAt)
                if (updated == 1) {
                    database.sessionDao().update(session.copy(updatedAt = revealedAt))
                    AnswerRevealWriteResult.Recorded
                } else {
                    AnswerRevealWriteResult.Unavailable
                }
            }
        }
}
