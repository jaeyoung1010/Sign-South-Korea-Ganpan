package com.signkorean.domain.port.output

interface SessionRepository {
    fun hasActiveSession(): Boolean
}
