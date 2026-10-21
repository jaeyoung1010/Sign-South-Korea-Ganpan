package com.signkorean.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReinsertionPolicyTest {
    @Test
    fun `reinserts a review card three to five cards later`() {
        val remaining = (1..10).map { "card-$it" }

        (1..9).forEach { reviewCount ->
            val queue = ReinsertionPolicy.reinsert("reviewed", remaining, reviewCount)
            val index = queue.indexOf("reviewed")

            assertTrue(index in ReinsertionPolicy.MIN_GAP..ReinsertionPolicy.MAX_GAP)
            assertEquals(remaining.size + 1, queue.size)
            assertEquals(remaining, queue.filterNot { it == "reviewed" })
        }
    }

    @Test
    fun `does not reinsert when fewer than the minimum cards remain`() {
        val remaining = listOf("card-1", "card-2")

        assertEquals(remaining, ReinsertionPolicy.reinsert("reviewed", remaining, reviewCount = 1))
    }
}
