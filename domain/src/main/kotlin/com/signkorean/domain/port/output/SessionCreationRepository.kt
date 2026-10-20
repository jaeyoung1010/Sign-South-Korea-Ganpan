package com.signkorean.domain.port.output

import com.signkorean.domain.model.SessionToCreate

/** Persists a complete session snapshot atomically with all of its ordered cards. */
interface SessionCreationRepository {
    suspend fun save(
        session: SessionToCreate,
        replaceActiveSession: Boolean,
    )
}
