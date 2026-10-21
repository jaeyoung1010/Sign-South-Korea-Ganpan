package com.signkorean.domain.model

import com.signkorean.domain.gesture.RatingDecision
import java.time.Instant

data class CardProgress(
    val cardId: String,
    val stage: Int,
    val mastered: Boolean,
    val lastRating: RatingDecision?,
    val lastReviewedAt: Instant?,
    val nextReviewAt: Instant?,
    val reviewCount: Int,
    val gotItCount: Int,
    val version: Int,
) {
    init {
        require(cardId.isNotBlank())
        require(stage >= 0 && reviewCount >= 0 && gotItCount >= 0 && version >= 0)
    }

    companion object {
        fun initial(cardId: String): CardProgress =
            CardProgress(
                cardId = cardId,
                stage = 0,
                mastered = false,
                lastRating = null,
                lastReviewedAt = null,
                nextReviewAt = null,
                reviewCount = 0,
                gotItCount = 0,
                version = 0,
            )
    }
}
