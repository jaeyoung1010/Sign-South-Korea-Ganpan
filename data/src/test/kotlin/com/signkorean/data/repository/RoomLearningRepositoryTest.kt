package com.signkorean.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.signkorean.data.db.CardProgressEntity
import com.signkorean.data.db.RoomSessionTransactionStore
import com.signkorean.data.db.SessionCardEntity
import com.signkorean.data.db.SessionEntity
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.RatingInputSource
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
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
