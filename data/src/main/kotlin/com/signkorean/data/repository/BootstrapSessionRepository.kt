package com.signkorean.data.repository

import com.signkorean.domain.port.output.SessionRepository

class BootstrapSessionRepository(
    private val activeSession: Boolean = false,
) : SessionRepository {
    override fun hasActiveSession(): Boolean = activeSession
}
