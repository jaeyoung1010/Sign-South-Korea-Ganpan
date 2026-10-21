package com.signkorean.domain.review

import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.CardProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `repeated got it follows the seven day interval at stage one`() {
        val previous =
            ReviewPolicy.apply(
                CardProgress.initial("card-1"),
                RatingDecision.GotIt,
                ratedAt,
            )

        val progress = ReviewPolicy.apply(previous, RatingDecision.GotIt, ratedAt.plusSeconds(THREE_DAYS))

        assertEquals(2, progress.stage)
        assertEquals(ratedAt.plusSeconds(THREE_DAYS + SEVEN_DAYS), progress.nextReviewAt)
        assertEquals(2, progress.gotItCount)
        assertEquals(2, progress.reviewCount)
        assertEquals(2, progress.version)
    }

    @Test
    fun `got it intervals progress through twenty one sixty and ninety days and master at stage five`() {
        var progress = CardProgress.initial("card-1")
        val expectedIntervals = listOf(THREE_DAYS, SEVEN_DAYS, TWENTY_ONE_DAYS, SIXTY_DAYS, NINETY_DAYS)

        expectedIntervals.forEachIndexed { index, interval ->
            val at = ratedAt.plusSeconds(expectedIntervals.take(index).sum())
            progress = ReviewPolicy.apply(progress, RatingDecision.GotIt, at)

            assertEquals(index + 1, progress.stage)
            assertEquals(at.plusSeconds(interval), progress.nextReviewAt)
            assertEquals(index == expectedIntervals.lastIndex, progress.mastered)
        }
    }

    @Test
    fun `mastered card remains mastered on maintenance got it and resets on review`() {
        var progress = CardProgress.initial("card-1")
        repeat(5) { index ->
            progress = ReviewPolicy.apply(progress, RatingDecision.GotIt, ratedAt.plusSeconds(index.toLong()))
        }
        assertTrue(progress.mastered)
        assertEquals(5, progress.stage)

        val maintenance = ReviewPolicy.apply(progress, RatingDecision.GotIt, ratedAt.plusSeconds(100))
        assertTrue(maintenance.mastered)
        assertEquals(5, maintenance.stage)
        assertEquals(ratedAt.plusSeconds(100 + NINETY_DAYS), maintenance.nextReviewAt)

        val retry = ReviewPolicy.apply(maintenance, RatingDecision.Review, ratedAt.plusSeconds(200))
        assertFalse(retry.mastered)
        assertEquals(0, retry.stage)
        assertEquals(ratedAt.plusSeconds(200 + ONE_DAY), retry.nextReviewAt)
    }

    @Test
    fun `instant based scheduling is stable across local midnight and daylight saving transitions`() {
        val beforeDst = Instant.parse("2026-03-28T23:30:00Z")
        val gotIt = ReviewPolicy.apply(CardProgress.initial("card-1"), RatingDecision.GotIt, beforeDst)

        assertEquals(beforeDst.plusSeconds(THREE_DAYS), gotIt.nextReviewAt)
        assertTrue(checkNotNull(gotIt.nextReviewAt).isAfter(beforeDst))

        val beforeMidnight = Instant.parse("2026-10-21T23:59:59Z")
        val review = ReviewPolicy.apply(gotIt, RatingDecision.Review, beforeMidnight)
        assertEquals(beforeMidnight.plusSeconds(ONE_DAY), review.nextReviewAt)
        assertTrue(checkNotNull(review.nextReviewAt).isAfter(beforeMidnight))
    }

    private companion object {
        const val ONE_DAY = 86_400L
        const val THREE_DAYS = ONE_DAY * 3
        const val SEVEN_DAYS = ONE_DAY * 7
        const val TWENTY_ONE_DAYS = ONE_DAY * 21
        const val SIXTY_DAYS = ONE_DAY * 60
        const val NINETY_DAYS = ONE_DAY * 90
    }
}
