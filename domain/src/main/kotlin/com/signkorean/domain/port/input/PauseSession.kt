package com.signkorean.domain.port.input

import com.signkorean.domain.model.ActiveSession

fun interface PauseSession {
    suspend operator fun invoke(request: PauseSessionRequest): PauseSessionResult
}

data class PauseSessionRequest(
    val sessionId: String,
) {
    init {
        require(sessionId.isNotBlank()) { "Session ID must not be blank" }
    }
}

sealed interface PauseSessionResult {
    data class Paused(
        val session: ActiveSession,
    ) : PauseSessionResult

    data class AlreadyPaused(
        val session: ActiveSession,
    ) : PauseSessionResult

    data object Unavailable : PauseSessionResult
}
