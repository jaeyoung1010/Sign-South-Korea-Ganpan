package com.signkorean.data.repository

import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.model.ActiveSession
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
}
