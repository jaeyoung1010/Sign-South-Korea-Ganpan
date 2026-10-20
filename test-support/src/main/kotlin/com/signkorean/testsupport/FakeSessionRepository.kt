package com.signkorean.testsupport

import com.signkorean.domain.port.output.SessionRepository

class FakeSessionRepository(
    var activeSession: Boolean = false,
) : SessionRepository {
    override suspend fun hasActiveSession(): Boolean = activeSession
}
