package com.signkorean.domain.model

/** Raw active-session data. It deliberately permits malformed persisted data so callers can recover safely. */
data class ActiveLearnSession(
    val sessionId: String,
    val currentIndex: Int,
    val requestedLength: Int,
    val cards: List<LearnSessionCard>,
)

data class LearnSessionCard(
    val position: Int,
    val cardId: String,
)

sealed interface LoadLearnSessionResult {
    data class Active(
        val sessionId: String,
        val card: SignCard,
        val position: Int,
        val totalCount: Int,
    ) : LoadLearnSessionResult {
        val progressFraction: Float
            get() = position.toFloat() / totalCount
    }

    data object Completed : LoadLearnSessionResult

    data class Invalid(
        val error: LearnSessionError,
    ) : LoadLearnSessionResult
}

enum class LearnSessionError {
    NoActiveSession,
    EmptySession,
    InvalidCardCount,
    InvalidCardOrder,
    InvalidCurrentIndex,
    UnknownCard,
    Unavailable,
}
