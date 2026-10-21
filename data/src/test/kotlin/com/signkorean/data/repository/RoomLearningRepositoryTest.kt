package com.signkorean.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.signkorean.data.db.CardProgressEntity
import com.signkorean.data.db.RatingSnapshotEntity
import com.signkorean.data.db.RoomSessionTransactionStore
import com.signkorean.data.db.SessionCardEntity
import com.signkorean.data.db.SessionEntity
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.RatingInputSource
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.input.UndoRatingRequest
import com.signkorean.domain.port.input.UndoRatingResult
import com.signkorean.testsupport.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomLearningRepositoryTest {
    private lateinit var database: SignKoreanDatabase
    private lateinit var repository: RoomLearningRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, SignKoreanDatabase::class.java).allowMainThreadQueries().build()
        val clock = FakeClock(Instant.parse("2026-10-21T00:00:00Z"))
        repository = RoomLearningRepository(database, RoomSessionTransactionStore(database, clock), Dispatchers.Unconfined)
        database.sessionDao().insert(SessionEntity.active("session-1", requestedLength = 2))
        database.sessionCardDao().insertAll(
            listOf(
                SessionCardEntity("session-1", 0, "card-1", "NEW"),
                SessionCardEntity("session-1", 1, "card-2", "NEW"),
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `rating persists progress snapshot and advances only one session position`() =
        runTest {
            val result = repository.rateCard(request(0, "card-1", RatingDecision.GotIt), ratedAt)

            assertEquals(RateCardResult.Rated(nextPosition = 1, completed = false), result)
            assertEquals(1, database.sessionDao().get("session-1")?.currentIndex)
            assertEquals("GOT_IT", database.sessionCardDao().get("session-1", 0)?.rating)
            assertEquals(1, database.cardProgressDao().get("card-1")?.stage)
            assertEquals("card-1", database.ratingSnapshotDao().forSession("session-1")?.cardId)
        }

    @Test
    fun `duplicate rating is rejected without changing progress`() =
        runTest {
            repository.rateCard(request(0, "card-1", RatingDecision.Review), ratedAt)
            val duplicate = repository.rateCard(request(0, "card-1", RatingDecision.GotIt), ratedAt.plusSeconds(1))

            assertEquals(RateCardResult.AlreadyRated, duplicate)
            assertEquals(1, database.cardProgressDao().get("card-1")?.reviewCount)
            assertEquals("REVIEW", database.sessionCardDao().get("session-1", 0)?.rating)
        }

    @Test
    fun `final rating returns an aggregate and removes the session from active work`() =
        runTest {
            repository.rateCard(request(0, "card-1", RatingDecision.GotIt), ratedAt)

            val result = repository.rateCard(request(1, "card-2", RatingDecision.Review), ratedAt.plusSeconds(30))

            assertTrue(result is RateCardResult.Rated)
            val completion = (result as RateCardResult.Rated).completion
            assertEquals(2, completion?.totalCount)
            assertEquals(1, completion?.gotItCount)
            assertEquals(1, completion?.reviewCount)
            assertEquals(listOf("card-2"), completion?.reviewCardIds)
            assertTrue(completion?.durationMillis ?: -1L >= 0L)
            assertEquals("COMPLETED", database.sessionDao().get("session-1")?.status)
            assertEquals(null, database.sessionDao().active())
        }

    @Test
    fun `database failure rolls back all rating writes and can be retried`() =
        runTest {
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_rating_once BEFORE UPDATE ON sessions
                WHEN NEW.current_index = 1 BEGIN SELECT RAISE(ABORT, 'stop'); END
                """.trimIndent(),
            )

            val failure = runCatching { repository.rateCard(request(0, "card-1", RatingDecision.GotIt), ratedAt) }

            assertTrue(failure.isFailure)
            assertNull(database.cardProgressDao().get("card-1"))
            assertNull(database.sessionCardDao().get("session-1", 0)?.rating)
            assertNull(database.ratingSnapshotDao().forSession("session-1"))
            assertEquals(0, database.sessionDao().get("session-1")?.currentIndex)
        }

    @Test
    fun `undo restores progress session index snapshot and answer state atomically`() =
        runTest {
            val revealedAt = ratedAt.minusSeconds(10)
            val previousProgress =
                CardProgressEntity(
                    cardId = "card-1",
                    stage = 2,
                    mastered = false,
                    lastRating = "GOT_IT",
                    lastReviewedAt = ratedAt.minusSeconds(100),
                    nextReviewAt = ratedAt.plusSeconds(100),
                    reviewCount = 3,
                    gotItCount = 2,
                    version = 3,
                )
            database.cardProgressDao().upsert(previousProgress)
            database.sessionCardDao().update(
                SessionCardEntity(
                    sessionId = "session-1",
                    position = 0,
                    cardId = "card-1",
                    source = "NEW",
                    answerRevealedAt = revealedAt,
                ),
            )
            repository.rateCard(request(0, "card-1", RatingDecision.Review), ratedAt)

            val result = repository.undoLastRating(UndoRatingRequest("session-1"))

            assertEquals(UndoRatingResult.Undone("card-1", 0, RatingDecision.Review), result)
            assertEquals(previousProgress, database.cardProgressDao().get("card-1"))
            assertEquals(0, database.sessionDao().get("session-1")?.currentIndex)
            assertEquals("ACTIVE", database.sessionDao().get("session-1")?.status)
            assertNull(database.sessionCardDao().get("session-1", 0)?.rating)
            assertEquals(revealedAt, database.sessionCardDao().get("session-1", 0)?.answerRevealedAt)
            assertNull(database.ratingSnapshotDao().forSession("session-1"))
        }

    @Test
    fun `undo with no snapshot is a safe no-op`() =
        runTest {
            assertEquals(UndoRatingResult.NothingToUndo, repository.undoLastRating(UndoRatingRequest("session-1")))
            assertEquals(0, database.sessionDao().get("session-1")?.currentIndex)
        }

    @Test
    fun `undo failure rolls back the original rating and snapshot`() =
        runTest {
            repository.rateCard(request(0, "card-1", RatingDecision.GotIt), ratedAt)
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_undo BEFORE UPDATE ON sessions
                WHEN NEW.current_index = 0 BEGIN SELECT RAISE(ABORT, 'stop'); END
                """.trimIndent(),
            )

            val failure = runCatching { repository.undoLastRating(UndoRatingRequest("session-1")) }

            assertTrue(failure.isFailure)
            assertEquals(1, database.sessionDao().get("session-1")?.currentIndex)
            assertEquals("GOT_IT", database.sessionCardDao().get("session-1", 0)?.rating)
            assertEquals(1, database.cardProgressDao().get("card-1")?.stage)
            assertEquals("card-1", database.ratingSnapshotDao().forSession("session-1")?.cardId)
        }

    @Test
    fun `undo rejects a corrupted rating without deleting the snapshot`() =
        runTest {
            database.sessionCardDao().update(
                SessionCardEntity(
                    sessionId = "session-1",
                    position = 0,
                    cardId = "card-1",
                    source = "NEW",
                    rating = "CORRUPT",
                    ratedAt = ratedAt,
                ),
            )
            database.sessionDao().update(database.sessionDao().get("session-1")!!.copy(currentIndex = 1))
            database.ratingSnapshotDao().upsert(
                RatingSnapshotEntity.noProgress("session-1", "card-1", 0),
            )

            val result = repository.undoLastRating(UndoRatingRequest("session-1"))

            assertEquals(UndoRatingResult.Unavailable, result)
            assertEquals("CORRUPT", database.sessionCardDao().get("session-1", 0)?.rating)
            assertEquals("card-1", database.ratingSnapshotDao().forSession("session-1")?.cardId)
            assertEquals(1, database.sessionDao().get("session-1")?.currentIndex)
        }

    private fun request(
        position: Int,
        cardId: String,
        decision: RatingDecision,
    ) = RateCardRequest("session-1", position, cardId, decision, RatingInputSource.Gesture)

    private companion object {
        val ratedAt = Instant.parse("2026-10-21T00:00:00Z")
    }

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}
