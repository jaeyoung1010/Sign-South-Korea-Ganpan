package com.signkorean.domain.session

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class DuePriorityTest {
    private val now = Instant.parse("2026-10-21T00:00:00Z")

    @Test
    fun `overdue cards come before due cards and higher review count wins within overdue cards`() {
        val cards =
            listOf(
                candidate("future", nextReviewAt = now.plusSeconds(10), reviewCount = 99),
                candidate("overdue-low", nextReviewAt = now.minusSeconds(20), reviewCount = 1),
                candidate("overdue-high", nextReviewAt = now.minusSeconds(10), reviewCount = 4),
            )

        assertEquals(
            listOf("overdue-high", "overdue-low", "future"),
            DuePriority.prioritize(cards, now).map(SessionCandidate::cardId),
        )
    }

    @Test
    fun `missing due metadata keeps the original stable order`() {
        val cards = listOf(candidate("a"), candidate("b"), candidate("c"))

        assertEquals(listOf("a", "b", "c"), DuePriority.prioritize(cards, now).map(SessionCandidate::cardId))
    }

    private fun candidate(
        id: String,
        nextReviewAt: Instant? = null,
        reviewCount: Int = 0,
    ) = SessionCandidate(
        cardId = id,
        order =
            when (id) {
                "a" -> 1
                "b" -> 2
                "c" -> 3
                else -> 0
            },
        category = "MARKET",
        isDue = true,
        isNew = false,
        nextReviewAt = nextReviewAt,
        reviewCount = reviewCount,
    )
}
