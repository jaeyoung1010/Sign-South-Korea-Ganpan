package com.signkorean.data.db

import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.SessionCardScore
import com.signkorean.domain.model.SessionCompletion

internal fun SignKoreanDatabase.toSessionCompletion(session: SessionEntity): SessionCompletion? {
    if (session.status != "COMPLETED" || session.completedAt == null) return null
    if (session.requestedLength <= 0 || session.currentIndex != session.requestedLength) return null

    val cards = sessionCardDao().forSession(session.sessionId)
    if (cards.size != session.requestedLength || cards.map(SessionCardEntity::position) != cards.indices.toList()) return null
    if (cards.any { it.rating !in RATINGS || it.ratedAt == null }) return null

    val scores =
        cards.map { card ->
            SessionCardScore(
                cardId = card.cardId,
                rating =
                    when (card.rating) {
                        "GOT_IT" -> RatingDecision.GotIt
                        "REVIEW" -> RatingDecision.Review
                        else -> return null
                    },
            )
        }
    val nextReviewAt = cards.mapNotNull { cardProgressDao().get(it.cardId)?.nextReviewAt }.minOrNull()
    return SessionCompletion(
        sessionId = session.sessionId,
        totalCount = cards.size,
        gotItCount = scores.count { it.rating == RatingDecision.GotIt },
        reviewCount = scores.count { it.rating == RatingDecision.Review },
        scores = scores,
        nextReviewAt = nextReviewAt,
        durationMillis = (session.completedAt.toEpochMilli() - session.createdAt.toEpochMilli()).coerceAtLeast(0L),
    )
}

private val RATINGS = setOf("GOT_IT", "REVIEW")
