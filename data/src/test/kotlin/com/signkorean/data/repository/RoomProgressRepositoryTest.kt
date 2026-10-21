package com.signkorean.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.signkorean.data.db.RoomSessionTransactionStore
import com.signkorean.data.db.SessionCardEntity
import com.signkorean.data.db.SessionEntity
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.CardProgress
import com.signkorean.testsupport.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomProgressRepositoryTest {
    private lateinit var database: SignKoreanDatabase
    private lateinit var repository: RoomLearningRepository
    private val now = Instant.parse("2026-10-20T03:00:00Z")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, SignKoreanDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomLearningRepository(database, RoomSessionTransactionStore(database, FakeClock(now)), Dispatchers.Unconfined)
        database.cardProgressDao().upsert(progress("card-1", nextReviewAt = now.minusSeconds(1)))
        database.cardProgressDao().upsert(progress("card-2", mastered = true, stage = 5, nextReviewAt = now.plusSeconds(1)))
        database.sessionDao().insert(SessionEntity.active("session-1", requestedLength = 4))
        database.sessionCardDao().insertAll(
            listOf(
                SessionCardEntity("session-1", 0, "card-1", "REVIEW", rating = "REVIEW", ratedAt = now.minusSeconds(10)),
                SessionCardEntity("session-1", 1, "card-1", "REVIEW", rating = "REVIEW", ratedAt = now),
                SessionCardEntity("session-1", 2, "card-2", "NEW", rating = "GOT_IT", ratedAt = now.minusSeconds(86_400)),
                SessionCardEntity("session-1", 3, "card-3", "NEW"),
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `room progress adapter exposes progress rows, unique recent reviews and all rating activity`() =
        runTest {
            val progress = repository.observeProgress(now).first()
            val recentReviews = repository.observeReviewCards(limit = 3, offset = 0).first()
            val activity = repository.observeRatingActivity().first()

            assertEquals(setOf("card-1", "card-2"), progress.map(CardProgress::cardId).toSet())
            assertEquals(listOf("card-1"), recentReviews.map { it.cardId })
            assertEquals(listOf("card-1", "card-1", "card-2"), activity.map { it.cardId })
            assertEquals(listOf(RatingDecision.Review, RatingDecision.Review, RatingDecision.GotIt), activity.map { it.decision })
        }

    @Test
    fun `undoing the latest review invalidates the reactive recent review query`() =
        runTest {
            val latest = database.sessionCardDao().get("session-1", 1)!!
            database.sessionCardDao().update(latest.copy(rating = null, ratedAt = null))

            val reviews = repository.observeReviewCards(limit = 3, offset = 0).first()

            assertEquals(listOf("card-1"), reviews.map { it.cardId })
        }

    private fun progress(
        cardId: String,
        stage: Int = 1,
        mastered: Boolean = false,
        nextReviewAt: Instant,
    ) =
        com.signkorean.data.db.CardProgressEntity(
            cardId = cardId,
            stage = stage,
            mastered = mastered,
            lastRating = "GOT_IT",
            lastReviewedAt = now,
            nextReviewAt = nextReviewAt,
            reviewCount = 1,
            gotItCount = 1,
            version = 1,
        )

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}
