package com.signkorean.domain.model

import com.signkorean.domain.gesture.RatingDecision
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SessionCompletionTest {
    @Test
    fun `supports the planned five ten and twenty card session sizes`() {
        listOf(5, 10, 20).forEach { total ->
            val scores =
                (0 until total).map { position ->
                    SessionCardScore("card-$position", if (position % 2 == 0) RatingDecision.GotIt else RatingDecision.Review)
                }
            val completion =
                SessionCompletion(
                    sessionId = "session-$total",
                    totalCount = total,
                    gotItCount = scores.count { it.rating == RatingDecision.GotIt },
                    reviewCount = scores.count { it.rating == RatingDecision.Review },
                    scores = scores,
                    nextReviewAt = Instant.parse("2026-10-22T00:00:00Z"),
                    durationMillis = 42_000,
                )

            assertEquals(total, completion.totalCount)
            assertEquals(scores.filter { it.rating == RatingDecision.Review }.map { it.cardId }, completion.reviewCardIds)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects aggregate counts that do not match card scores`() {
        SessionCompletion(
            sessionId = "session-1",
            totalCount = 2,
            gotItCount = 2,
            reviewCount = 0,
            scores = listOf(SessionCardScore("card-1", RatingDecision.Review), SessionCardScore("card-2", RatingDecision.GotIt)),
            nextReviewAt = null,
            durationMillis = 0,
        )
    }
}
