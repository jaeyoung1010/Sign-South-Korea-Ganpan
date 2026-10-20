package com.signkorean.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.signkorean.data.db.CardProgressEntity
import com.signkorean.data.db.SessionCardEntity
import com.signkorean.data.db.SessionEntity
import com.signkorean.data.db.SignKoreanDatabase
import kotlinx.coroutines.Dispatchers
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
class RoomHomeActivityRepositoryTest {
    private lateinit var database: SignKoreanDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, SignKoreanDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `loads persisted progressed due and reviewed cards for the home dashboard`() =
        runTest {
            database.cardProgressDao().upsert(progress("due-card", nextReviewAt = at(10), lastReviewedAt = at(1)))
            database.cardProgressDao().upsert(progress("newer-card", nextReviewAt = at(30), lastReviewedAt = at(2)))
            database.sessionDao().insert(SessionEntity.active(id = "session", requestedLength = 2))
            database.sessionCardDao().insertAll(
                listOf(
                    SessionCardEntity("session", 0, "due-card", "REVIEW", rating = "GOT_IT", ratedAt = at(1)),
                    SessionCardEntity("session", 1, "due-card", "REVIEW", rating = "GOT_IT", ratedAt = at(2)),
                ),
            )

            val snapshot = RoomHomeActivityRepository(database, Dispatchers.Unconfined).loadHomeActivity(at(20))

            assertEquals(setOf("due-card", "newer-card"), snapshot.progressedCardIds)
            assertEquals(setOf("due-card"), snapshot.dueCardIds)
            assertEquals(listOf(at(1), at(2)), snapshot.reviewedAt)
        }

    private fun progress(
        cardId: String,
        nextReviewAt: Instant,
        lastReviewedAt: Instant,
    ) = CardProgressEntity(
        cardId = cardId,
        stage = 1,
        mastered = false,
        lastRating = "GOT_IT",
        lastReviewedAt = lastReviewedAt,
        nextReviewAt = nextReviewAt,
        reviewCount = 0,
        gotItCount = 1,
        version = 1,
    )

    private fun at(seconds: Long): Instant = Instant.ofEpochSecond(seconds)

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}
