package com.signkorean.domain.port.output

interface SessionRepository {
    suspend fun hasActiveSession(): Boolean
}
