package com.signkorean.domain.usecase

import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.BundleValidationReport
import com.signkorean.domain.model.CardProgress
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.LocalizedCardCopy
import com.signkorean.domain.model.ProgressReview
import com.signkorean.domain.model.RatingActivity
import com.signkorean.domain.model.SignCard
import com.signkorean.domain.port.input.ReviewPageRequest
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.LearningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ObserveProgressUseCaseTest {
    private val now = Instant.parse("2026-10-20T03:00:00Z")
    private val zone = ZoneId.of("Asia/Seoul")

    @Test
    fun `aggregates learned mastered due seven days categories and recent reviews from stored data`() =
        runTest {
            val repository =
                FakeLearningRepository(
                    progress =
                        listOf(
                            progress("market-1", stage = 1, nextReviewAt = now.minusSeconds(1)),
                            progress("market-2", stage = 5, mastered = true, nextReviewAt = now.plusSeconds(1)),
                            progress("transport-1", stage = 0, nextReviewAt = now),
                        ),
                    activity =
                        listOf(
                            activity("market-1", RatingDecision.GotIt, now),
                            activity("market-1", RatingDecision.Review, now),
                            activity("transport-1", RatingDecision.Review, now.minusSeconds(86_400)),
                            activity("transport-1", RatingDecision.GotIt, now.minusSeconds(8 * 86_400)),
                        ),
                    reviews =
                        listOf(
                            ProgressReview("market-1", now),
                            ProgressReview("transport-1", now.minusSeconds(86_400)),
                        ),
                )

            val snapshot = observe(repository).first()

            assertEquals(3, snapshot.learnedCount)
            assertEquals(1, snapshot.masteredCount)
            assertEquals(2, snapshot.dueCount)
            assertEquals(listOf(1, 2), snapshot.weeklyActivity.map { it.count }.filter { it > 0 })
            assertEquals(
                listOf("MARKET" to (2 to 1f), "TRANSPORT" to (1 to 1f)),
                snapshot.categoryProgress.map { it.key to (it.learnedCount to it.ratio) },
            )
            assertEquals(listOf("market-1", "transport-1"), snapshot.recentReviews.map(ProgressReview::cardId))
        }

    @Test
    fun `empty data returns zero statistics and a seven day zero chart`() =
        runTest {
            val snapshot = observe(FakeLearningRepository()).first()

            assertEquals(0, snapshot.learnedCount)
            assertEquals(0, snapshot.masteredCount)
            assertEquals(0, snapshot.dueCount)
            assertEquals(7, snapshot.weeklyActivity.size)
            assertTrue(snapshot.weeklyActivity.all { it.count == 0 })
            assertTrue(snapshot.recentReviews.isEmpty())
            assertTrue(snapshot.categoryProgress.all { it.learnedCount == 0 })
        }

    @Test
    fun `duplicate progress rows do not inflate learned mastered or due counts`() =
        runTest {
            val duplicate = progress("market-1", stage = 5, mastered = true, nextReviewAt = now)
            val snapshot = observe(FakeLearningRepository(progress = listOf(duplicate, duplicate))).first()

            assertEquals(1, snapshot.learnedCount)
            assertEquals(1, snapshot.masteredCount)
            assertEquals(1, snapshot.dueCount)
        }

    @Test
    fun `undo is reflected by the same reactive aggregation flow`() =
        runTest {
            val repository =
                FakeLearningRepository(
                    progress = listOf(progress("market-1", stage = 1, nextReviewAt = now)),
                    activity = listOf(activity("market-1", RatingDecision.GotIt, now)),
                    reviews = emptyList(),
                )
            val snapshots = observe(repository)

            assertEquals(1, snapshots.first().learnedCount)
            repository.progress.value = emptyList()
            repository.activity.value = emptyList()

            assertEquals(0, snapshots.first().learnedCount)
            assertEquals(0, snapshots.first().weeklyActivity.sumOf { it.count })
        }

    @Test
    fun `recent review output is capped at three while paging keeps the full repository query`() =
        runTest {
            val allReviews = (0 until 5).map { ProgressReview("market-$it", now.minusSeconds(it.toLong())) }
            val repository = FakeLearningRepository(reviews = allReviews)
            val recent = observe(repository).first().recentReviews
            val page = ObserveReviewCardsUseCase(repository)(ReviewPageRequest(limit = 5, offset = 3)).first()

            assertEquals(3, recent.size)
            assertEquals(listOf("market-3", "market-4"), page.map(ProgressReview::cardId))
        }

    private fun observe(repository: LearningRepository): Flow<com.signkorean.domain.model.ProgressSnapshot> =
        ObserveProgressUseCase(repository, FixtureCatalog(), FixedClock())()

    private fun progress(
        cardId: String,
        stage: Int,
        mastered: Boolean = false,
        nextReviewAt: Instant,
    ) = CardProgress(
        cardId = cardId,
        stage = stage,
        mastered = mastered,
        lastRating = RatingDecision.GotIt,
        lastReviewedAt = now,
        nextReviewAt = nextReviewAt,
        reviewCount = 1,
        gotItCount = 1,
        version = 1,
    )

    private fun activity(cardId: String, decision: RatingDecision, ratedAt: Instant) =
        RatingActivity(cardId, decision, ratedAt)

    private class FakeLearningRepository(
        progress: List<CardProgress> = emptyList(),
        activity: List<RatingActivity> = emptyList(),
        reviews: List<ProgressReview> = emptyList(),
    ) : LearningRepository {
        val progress = MutableStateFlow(progress)
        val activity = MutableStateFlow(activity)
        private val reviews = MutableStateFlow(reviews)

        override fun getReviewState(cardId: String) = com.signkorean.domain.model.CardReviewState.NotFound

        override fun observeProgress(now: Instant): Flow<List<CardProgress>> = progress

        override fun observeRatingActivity(): Flow<List<RatingActivity>> = activity

        override fun observeReviewCards(limit: Int, offset: Int): Flow<List<ProgressReview>> =
            flowOf(reviews.value.drop(offset).take(limit))
    }

    private class FixtureCatalog : CardCatalog {
        override fun getCards(): List<SignCard> =
            listOf(
                card("market-1", "MARKET"),
                card("market-2", "MARKET"),
                card("transport-1", "TRANSPORT"),
            )

        override fun getCard(cardId: String): SignCard? = getCards().firstOrNull { it.id == cardId }

        override fun getLocalizedCopy(languageTag: String): List<LocalizedCardCopy> = emptyList()

        override fun validateBundle() = BundleValidationReport(3, 3, 3, setOf("en"))

        private fun card(id: String, category: String) =
            SignCard(id, 0, id, category, "asset://$id", FocusRect(0f, 0f, 1f, 1f), emptyMap(), 1)
    }

    private inner class FixedClock : ClockPort {
        override fun now(): Instant = now

        override fun zoneId(): ZoneId = zone
    }
}
