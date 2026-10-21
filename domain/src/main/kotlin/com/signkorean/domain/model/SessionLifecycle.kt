package com.signkorean.domain.model

data class SessionPausedEvent(
    val position: Int,
    val sessionLength: Int,
) {
    init {
        require(position in 0 until sessionLength) { "Position must be within the session length" }
        require(sessionLength > 0) { "Session length must be positive" }
    }
}

data class SessionResumedEvent(
    val position: Int,
    val sessionLength: Int,
) {
    init {
        require(position in 0 until sessionLength) { "Position must be within the session length" }
        require(sessionLength > 0) { "Session length must be positive" }
    }
}

sealed interface SessionTransitionWriteResult {
    data class Changed(
        val session: ActiveSession,
    ) : SessionTransitionWriteResult

    data class AlreadyInState(
        val session: ActiveSession,
    ) : SessionTransitionWriteResult

    data object Unavailable : SessionTransitionWriteResult
}
