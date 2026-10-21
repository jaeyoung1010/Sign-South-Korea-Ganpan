package com.signkorean.domain.model

import com.signkorean.domain.gesture.RatingDecision
import java.time.Instant

data class SessionCardScore(
    val cardId: String,
    val rating: RatingDecision,
) {
    init {
        require(cardId.isNotBlank())
    }
}

data class SessionCompletion(
    val sessionId: String,
    val totalCount: Int,
    val gotItCount: Int,
    val reviewCount: Int,
    val scores: List<SessionCardScore>,
    val nextReviewAt: Instant?,
    val durationMillis: Long,
) {
    init {
        require(sessionId.isNotBlank())
        require(totalCount > 0)
        require(gotItCount >= 0)
        require(reviewCount >= 0)
        require(gotItCount + reviewCount == totalCount)
        require(scores.size == totalCount)
        require(scores.count { it.rating == RatingDecision.GotIt } == gotItCount)
        require(scores.count { it.rating == RatingDecision.Review } == reviewCount)
        require(durationMillis >= 0)
    }

    val reviewCardIds: List<String>
        get() = scores.filter { it.rating == RatingDecision.Review }.map(SessionCardScore::cardId)
}
