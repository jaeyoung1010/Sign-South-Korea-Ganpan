package com.signkorean.testsupport

import com.signkorean.domain.port.output.SessionRepository

class FakeSessionRepository(
    var activeSession: Boolean = false,
) : SessionRepository {
    override fun hasActiveSession(): Boolean = activeSession
}
