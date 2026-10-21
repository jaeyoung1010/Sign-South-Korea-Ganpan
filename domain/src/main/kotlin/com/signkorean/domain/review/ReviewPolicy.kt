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
        val nextStage =
            if (isGotIt) {
                (current.stage + 1).coerceAtMost(MASTERED_STAGE)
            } else {
                0
            }
        val intervalSeconds =
            if (isGotIt) {
                intervalForStage(current.stage)
            } else {
                ONE_DAY_SECONDS
            }
        return current.copy(
            stage = nextStage,
            mastered = isGotIt && nextStage >= MASTERED_STAGE,
            lastRating = decision,
            lastReviewedAt = ratedAt,
            nextReviewAt = ratedAt.plusSeconds(intervalSeconds),
            reviewCount = current.reviewCount + 1,
            gotItCount = current.gotItCount + if (isGotIt) 1 else 0,
            version = current.version + 1,
        )
    }

    private fun intervalForStage(stage: Int): Long =
        when (stage.coerceAtLeast(0)) {
            0 -> THREE_DAYS_SECONDS
            1 -> SEVEN_DAYS_SECONDS
            2 -> TWENTY_ONE_DAYS_SECONDS
            3 -> SIXTY_DAYS_SECONDS
            else -> NINETY_DAYS_SECONDS
        }

    private const val ONE_DAY_SECONDS = 86_400L
    private const val THREE_DAYS_SECONDS = ONE_DAY_SECONDS * 3
    private const val SEVEN_DAYS_SECONDS = ONE_DAY_SECONDS * 7
    private const val TWENTY_ONE_DAYS_SECONDS = ONE_DAY_SECONDS * 21
    private const val SIXTY_DAYS_SECONDS = ONE_DAY_SECONDS * 60
    private const val NINETY_DAYS_SECONDS = ONE_DAY_SECONDS * 90
    private const val MASTERED_STAGE = 5
}
