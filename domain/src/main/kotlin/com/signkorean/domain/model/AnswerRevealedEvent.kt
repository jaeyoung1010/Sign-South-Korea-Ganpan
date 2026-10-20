package com.signkorean.domain.model

/** Analytics payload deliberately limited to the card and its one-based session position. */
data class AnswerRevealedEvent(
    val cardId: String,
    val position: Int,
)

/** Keeps a stale persistence rejection distinct from an idempotent repeated reveal. */
enum class AnswerRevealWriteResult {
    Recorded,
    AlreadyRecorded,
    Unavailable,
}
