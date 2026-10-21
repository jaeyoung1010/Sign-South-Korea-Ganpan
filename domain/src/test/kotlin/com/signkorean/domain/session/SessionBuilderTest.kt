package com.signkorean.domain.session

import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import java.time.Instant

class SessionBuilderTest {
    private val builder = SessionBuilder()

    @Test
    fun `builds each supported daily length without exceeding the distinct candidate pool`() {
        val candidates = candidates(count = 20)

        listOf(5, 10, 20).forEach { length ->
            val plan = builder.build(dailyRequest(length = length, candidates = candidates))

            assertEquals(length, plan.cardIds.size)
            assertEquals(length, plan.cardIds.toSet().size)
        }
    }

    @Test
    fun `daily session uses zero thirty and seventy percent due mixes`() {
        val length = 10
        val noDueCandidates = candidates(20, dueCount = 0)
        val thirtyPercentDueCandidates = candidates(20, dueCount = 3)
        val seventyPercentDueCandidates = candidates(20, dueCount = 7)

        val noDue = builder.build(dailyRequest(length, candidates = noDueCandidates))
        val thirtyPercentDue = builder.build(dailyRequest(length, candidates = thirtyPercentDueCandidates))
        val seventyPercentDue = builder.build(dailyRequest(length, candidates = seventyPercentDueCandidates))

        assertEquals(0, dueCount(noDue, noDueCandidates))
        assertEquals(3, dueCount(thirtyPercentDue, thirtyPercentDueCandidates))
        assertEquals(7, dueCount(seventyPercentDue, seventyPercentDueCandidates))
    }

    @Test
    fun `category session places category cards before fallback cards`() {
        val candidates =
            candidates(count = 8).mapIndexed { index, candidate ->
                candidate.copy(category = if (index < 3) "MARKET" else "TRANSPORT")
            }

        val plan =
            builder.build(
                SessionBuildRequest.category(
                    length = 5,
                    category = "MARKET",
                    candidates = candidates,
                    seed = 4L,
                ),
            )

        assertEquals(listOf("card-0", "card-1", "card-2"), plan.cardIds.take(3))
        assertEquals(ReviewReinsertionPolicy.RUNTIME_QUEUE, plan.reviewReinsertionPolicy)
    }

    @Test
    fun `daily session prioritizes overdue cards and then review count`() {
        val now = Instant.parse("2026-10-21T00:00:00Z")
        val candidates =
            listOf(
                candidate("future", 0, isDue = true).copy(nextReviewAt = now.plusSeconds(10), reviewCount = 99),
                candidate("overdue-low", 1, isDue = true).copy(nextReviewAt = now.minusSeconds(20), reviewCount = 1),
                candidate("overdue-high", 2, isDue = true).copy(nextReviewAt = now.minusSeconds(10), reviewCount = 4),
                candidate("new", 3),
            )

        val plan =
            builder.build(
                SessionBuildRequest.daily(
                    length = 5,
                    candidates = candidates,
                    interests = emptySet(),
                    level = LearningLevel.BEGINNER,
                    seed = 1L,
                    now = now,
                ),
            )

        assertEquals(listOf("overdue-high", "overdue-low", "future"), plan.cardIds.take(3))
    }

    @Test
    fun `never duplicates cards and returns available cards when content is insufficient`() {
        val candidates =
            listOf(
                candidate(id = "a", order = 1),
                candidate(id = "a", order = 2, isDue = true),
                candidate(id = "b", order = 3),
                candidate(id = "c", order = 4),
            )

        val plan = builder.build(dailyRequest(length = 10, candidates = candidates))

        assertEquals(listOf("a", "b", "c"), plan.cardIds)
        assertEquals(plan.cardIds.size, plan.cardIds.toSet().size)
    }

    @Test
    fun `duplicate candidates retain a due state from any matching record`() {
        val candidates =
            listOf(
                candidate(id = "a", order = 5),
                candidate(id = "b", order = 1),
                candidate(id = "a", order = 5, isDue = true),
            )

        val plan = builder.build(dailyRequest(length = 5, candidates = candidates))

        assertEquals("a", plan.cardIds.first())
        assertEquals(2, plan.cardIds.toSet().size)
    }

    @Test
    fun `same seed creates the same ordered session`() {
        val request = dailyRequest(length = 10, candidates = candidates(20, dueCount = 7), seed = 99L)

        assertEquals(builder.build(request), builder.build(request))
    }

    @Test
    fun `interests and level influence the ordering of new cards`() {
        val interestCandidates =
            candidates(count = 10).mapIndexed { index, candidate ->
                candidate.copy(category = if (index < 5) "TRANSPORT" else "MARKET")
            }
        val allMarketCandidates = candidates(count = 10).map { it.copy(category = "MARKET") }

        val interested = builder.build(dailyRequest(length = 5, candidates = interestCandidates))
        val beginner =
            builder.build(
                SessionBuildRequest.daily(
                    length = 5,
                    candidates = allMarketCandidates,
                    interests = emptySet(),
                    level = LearningLevel.BEGINNER,
                    seed = 1L,
                ),
            )
        val elementary =
            builder.build(
                SessionBuildRequest.daily(
                    length = 5,
                    candidates = allMarketCandidates,
                    interests = emptySet(),
                    level = LearningLevel.ELEMENTARY,
                    seed = 1L,
                ),
            )

        assertTrue(interested.cardIds.all { it.removePrefix("card-").toInt() >= 5 })
        assertEquals("card-0", beginner.cardIds.first())
        assertEquals("card-9", elementary.cardIds.first())
    }

    @Test
    fun `practice session contains only the requested card`() {
        val plan =
            builder.build(
                SessionBuildRequest.practice(
                    cardId = "card-7",
                    candidates = candidates(10),
                    seed = 3L,
                ),
            )

        assertEquals(listOf("card-7"), plan.cardIds)
    }

    @Test
    fun `twenty card basic bundle contains every unique card once`() {
        val candidates = candidates(count = 20)

        val plan = builder.build(dailyRequest(length = 20, candidates = candidates))

        assertEquals(candidates.map { it.cardId }.toSet(), plan.cardIds.toSet())
        assertEquals(20, plan.cardIds.size)
    }

    @Test
    fun `ten thousand randomized inputs stay within their available distinct card pools`() {
        val random = Random(4_216L)

        repeat(10_000) {
            val poolSize = random.nextInt(31)
            val requestedLength = listOf(5, 10, 20)[random.nextInt(3)]
            val candidates =
                (0 until poolSize).map { index ->
                    candidate(
                        id = "random-$it-$index",
                        order = index,
                        category = if (index % 2 == 0) "MARKET" else "TRANSPORT",
                        isDue = random.nextBoolean(),
                        isNew = random.nextBoolean(),
                    )
                }
            val request =
                when (random.nextInt(3)) {
                    0 -> {
                        dailyRequest(requestedLength, candidates, random.nextLong())
                    }

                    1 -> {
                        SessionBuildRequest.category(
                            length = requestedLength,
                            category = "MARKET",
                            candidates = candidates,
                            seed = random.nextLong(),
                        )
                    }

                    else -> {
                        SessionBuildRequest.practice(
                            cardId = candidates.firstOrNull()?.cardId ?: "missing",
                            candidates = candidates,
                            seed = random.nextLong(),
                        )
                    }
                }

            val plan = builder.build(request)
            val availableIds = candidates.map { it.cardId }.toSet()

            assertTrue(plan.cardIds.size <= minOf(requestedLength, availableIds.size))
            assertEquals(plan.cardIds.size, plan.cardIds.toSet().size)
            assertTrue(plan.cardIds.all(availableIds::contains))
            assertFalse(plan.cardIds.any { it.isBlank() })
        }
    }

    private fun dueCount(
        plan: SessionPlan,
        candidates: List<SessionCandidate>,
    ): Int {
        val dueIds = candidates.filter(SessionCandidate::isDue).mapTo(mutableSetOf(), SessionCandidate::cardId)
        return plan.cardIds.count { it in dueIds }
    }

    private fun dailyRequest(
        length: Int,
        candidates: List<SessionCandidate>,
        seed: Long = 1L,
    ): SessionBuildRequest =
        SessionBuildRequest.daily(
            length = length,
            candidates = candidates,
            interests = setOf(Interest.MARKET),
            level = LearningLevel.BEGINNER,
            seed = seed,
        )

    private fun candidates(
        count: Int,
        dueCount: Int = 0,
    ): List<SessionCandidate> =
        (0 until count).map { index ->
            candidate(
                id = "card-$index",
                order = index,
                category = if (index % 2 == 0) "MARKET" else "TRANSPORT",
                isDue = index < dueCount,
            )
        }

    private fun candidate(
        id: String,
        order: Int,
        category: String = "MARKET",
        isDue: Boolean = false,
        isNew: Boolean = true,
    ) = SessionCandidate(
        cardId = id,
        order = order,
        category = category,
        isDue = isDue,
        isNew = isNew,
    )
}
