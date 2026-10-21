package com.signkorean.domain.review

import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.CardProgress
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ReviewPolicyTest {
    private val ratedAt = Instant.parse("2026-10-21T00:00:00Z")

    @Test
    fun `first got it advances to stage one and is due in three days`() {
        val progress = ReviewPolicy.apply(cardId = "card-1", previous = null, decision = RatingDecision.GotIt, ratedAt = ratedAt)

        assertEquals(1, progress.stage)
        assertEquals(ratedAt.plusSeconds(THREE_DAYS), progress.nextReviewAt)
        assertEquals(1, progress.gotItCount)
        assertEquals(1, progress.reviewCount)
    }

    @Test
    fun `review keeps stage zero and is due tomorrow`() {
        val previous = CardProgress.initial("card-1")

        val progress = ReviewPolicy.apply(previous, RatingDecision.Review, ratedAt)

        assertEquals(0, progress.stage)
        assertEquals(ratedAt.plusSeconds(ONE_DAY), progress.nextReviewAt)
        assertEquals(0, progress.gotItCount)
        assertEquals(1, progress.reviewCount)
    }

    @Test
    fun `repeated got it increments version without losing previous counts`() {
        val previous =
            ReviewPolicy.apply(
                CardProgress.initial("card-1"),
                RatingDecision.GotIt,
                ratedAt,
            )

        val progress = ReviewPolicy.apply(previous, RatingDecision.GotIt, ratedAt.plusSeconds(THREE_DAYS))

        assertEquals(2, progress.stage)
        assertEquals(2, progress.gotItCount)
        assertEquals(2, progress.reviewCount)
        assertEquals(2, progress.version)
    }

    private companion object {
        const val ONE_DAY = 86_400L
        const val THREE_DAYS = ONE_DAY * 3
    }
}
