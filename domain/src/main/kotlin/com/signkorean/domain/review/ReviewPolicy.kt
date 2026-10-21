package com.signkorean.domain.review

import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.CardProgress
import java.time.Instant

/** First-pass scheduling policy used by the learning transaction. */
object ReviewPolicy {
    fun apply(
        previous: CardProgress?,
        decision: RatingDecision,
        ratedAt: Instant,
    ): CardProgress {
        val current = requireNotNull(previous) { "A card id is required for a new progress row" }
        return apply(current.cardId, current, decision, ratedAt)
    }

    fun apply(
        cardId: String,
        previous: CardProgress?,
        decision: RatingDecision,
        ratedAt: Instant,
    ): CardProgress {
        val current = previous ?: CardProgress.initial(cardId)
        val isGotIt = decision == RatingDecision.GotIt
        return current.copy(
            stage = if (isGotIt) current.stage + 1 else 0,
            mastered = false,
            lastRating = decision,
            lastReviewedAt = ratedAt,
            nextReviewAt = ratedAt.plusSeconds(if (isGotIt) THREE_DAYS_SECONDS else ONE_DAY_SECONDS),
            reviewCount = current.reviewCount + 1,
            gotItCount = current.gotItCount + if (isGotIt) 1 else 0,
            version = current.version + 1,
        )
    }

    private const val ONE_DAY_SECONDS = 86_400L
    private const val THREE_DAYS_SECONDS = ONE_DAY_SECONDS * 3
}
