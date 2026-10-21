package com.signkorean.domain.port.input

import com.signkorean.domain.model.ActiveSession

fun interface ResumeSession {
    suspend operator fun invoke(request: ResumeSessionRequest): ResumeSessionResult
}

data class ResumeSessionRequest(
    val sessionId: String,
) {
    init {
        require(sessionId.isNotBlank()) { "Session ID must not be blank" }
    }
}

sealed interface ResumeSessionResult {
    data class Resumed(
        val session: ActiveSession,
    ) : ResumeSessionResult

    data class AlreadyActive(
        val session: ActiveSession,
    ) : ResumeSessionResult

    data object Unavailable : ResumeSessionResult
}
