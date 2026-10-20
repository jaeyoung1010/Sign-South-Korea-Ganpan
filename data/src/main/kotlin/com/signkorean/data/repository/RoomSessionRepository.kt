package com.signkorean.data.repository

import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.model.ActiveLearnSession
import com.signkorean.domain.model.ActiveSession
import com.signkorean.domain.model.LearnSessionCard
import com.signkorean.domain.port.output.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

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
                            LearnSessionCard(position = card.position, cardId = card.cardId)
                        },
                )
            }
        }
}
