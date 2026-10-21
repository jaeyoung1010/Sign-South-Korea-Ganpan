package com.signkorean.data.repository

import com.signkorean.data.db.SessionEntity
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.data.db.isConsistent
import com.signkorean.data.db.isStructurallyConsistent
import com.signkorean.data.db.toSessionCompletion
import com.signkorean.data.time.SystemClockAdapter
import com.signkorean.domain.model.ActiveLearnSession
import com.signkorean.domain.model.ActiveSession
import com.signkorean.domain.model.AnswerRevealWriteResult
import com.signkorean.domain.model.LearnSessionCard
import com.signkorean.domain.model.SessionTransitionWriteResult
import com.signkorean.domain.model.SessionCompletion
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.SessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant

class RoomSessionRepository(
    private val database: SignKoreanDatabase,
    private val dispatcher: CoroutineDispatcher,
    private val clock: ClockPort = SystemClockAdapter(),
) : SessionRepository {
    override suspend fun hasActiveSession(): Boolean =
        withContext(dispatcher) {
            database.validResumableSession() != null
        }

    override suspend fun getActiveSession(): ActiveSession? =
        withContext(dispatcher) {
            database.validResumableSession()?.let { session ->
                session.toActiveSession()
            }
        }

    override suspend fun getActiveLearnSession(): ActiveLearnSession? =
        withContext(dispatcher) {
            database.validActiveSession()?.let { session ->
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

    private fun SignKoreanDatabase.validResumableSession(): SessionEntity? {
        val session = sessionDao().resumable() ?: return null
        if (isConsistent(session)) return session
        quarantine(session)
        return null
    }

    private fun SignKoreanDatabase.validActiveSession(): SessionEntity? {
        val session = sessionDao().active() ?: return null
        if (isStructurallyConsistent(session)) return session
        quarantine(session)
        return null
    }

    private fun SignKoreanDatabase.quarantine(session: SessionEntity) {
        try {
            sessionDao().update(session.copy(status = ABANDONED_STATUS, updatedAt = clock.now()))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // A failed quarantine must not turn recoverable startup data loss into a startup crash.
        }
    }

    override suspend fun getSessionCompletion(sessionId: String): SessionCompletion? =
        withContext(dispatcher) {
            database.sessionDao().get(sessionId)?.let(database::toSessionCompletion)
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

    override suspend fun pauseSession(sessionId: String): SessionTransitionWriteResult =
        withContext(dispatcher) {
            database.runInTransaction<SessionTransitionWriteResult> {
                val session = database.sessionDao().get(sessionId) ?: return@runInTransaction SessionTransitionWriteResult.Unavailable
                if (!database.isConsistent(session)) return@runInTransaction SessionTransitionWriteResult.Unavailable
                when (session.status) {
                    ACTIVE_STATUS -> {
                        val paused = session.copy(status = PAUSED_STATUS, updatedAt = clock.now())
                        database.sessionDao().update(paused)
                        SessionTransitionWriteResult.Changed(paused.toActiveSession())
                    }

                    PAUSED_STATUS -> {
                        SessionTransitionWriteResult.AlreadyInState(session.toActiveSession())
                    }

                    else -> {
                        SessionTransitionWriteResult.Unavailable
                    }
                }
            }
        }

    override suspend fun resumeSession(sessionId: String): SessionTransitionWriteResult =
        withContext(dispatcher) {
            database.runInTransaction<SessionTransitionWriteResult> {
                val session = database.sessionDao().get(sessionId) ?: return@runInTransaction SessionTransitionWriteResult.Unavailable
                if (!database.isConsistent(session)) return@runInTransaction SessionTransitionWriteResult.Unavailable
                when (session.status) {
                    PAUSED_STATUS -> {
                        val resumed = session.copy(status = ACTIVE_STATUS, updatedAt = clock.now())
                        database.sessionDao().update(resumed)
                        SessionTransitionWriteResult.Changed(resumed.toActiveSession())
                    }

                    ACTIVE_STATUS -> {
                        SessionTransitionWriteResult.AlreadyInState(session.toActiveSession())
                    }

                    else -> {
                        SessionTransitionWriteResult.Unavailable
                    }
                }
            }
        }

    private fun SessionEntity.toActiveSession() =
        ActiveSession(
            completedCount = currentIndex,
            totalCount = requestedLength,
            sessionId = sessionId,
            isPaused = status == PAUSED_STATUS,
        )

    private companion object {
        const val ACTIVE_STATUS = "ACTIVE"
        const val PAUSED_STATUS = "PAUSED"
        const val ABANDONED_STATUS = "ABANDONED"
    }
}
