package com.signkorean.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomLearningRepositoryReinsertionTest {
    private lateinit var database: SignKoreanDatabase
    private lateinit var repository: RoomLearningRepository
    private val ratedAt = Instant.parse("2026-10-21T00:00:00Z")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, SignKoreanDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomLearningRepository(database, RoomSessionTransactionStore(database, FakeClock(ratedAt)), Dispatchers.Unconfined)
        database.sessionDao().insert(SessionEntity.active("session-1", requestedLength = 6))
        database.sessionCardDao().insertAll(
            (0 until 6).map { position -> SessionCardEntity("session-1", position, "card-$position", "NEW") },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `review card is reinserted three cards later and session length grows`() =
        runTest {
            val result = repository.rateCard(request(0, "card-0"), ratedAt)

            assertTrue(result is RateCardResult.Rated)
            assertEquals(7, database.sessionCardDao().countForSession("session-1"))
            assertEquals(7, database.sessionDao().get("session-1")?.requestedLength)
            assertEquals(1, database.sessionDao().get("session-1")?.currentIndex)
            assertEquals("card-0", database.sessionCardDao().get("session-1", 4)?.cardId)
            assertEquals("card-5", database.sessionCardDao().get("session-1", 6)?.cardId)
        }

    @Test
    fun `review is not reinserted when fewer than three cards remain`() =
        runTest {
            database.sessionCardDao().forSession("session-1").filter { it.position >= 3 }.forEach {
                database.sessionCardDao().delete(it.sessionId, it.position)
            }
            database.sessionDao().update(database.sessionDao().get("session-1")!!.copy(requestedLength = 3))

            repository.rateCard(request(0, "card-0"), ratedAt)

            assertEquals(3, database.sessionCardDao().countForSession("session-1"))
            assertEquals(3, database.sessionDao().get("session-1")?.requestedLength)
        }

    @Test
    fun `runtime reinsertion still allows the expanded session to complete`() =
        runTest {
            repository.rateCard(request(0, "card-0"), ratedAt)
            (1..5).forEach { position ->
                val cardId = database.sessionCardDao().get("session-1", position)!!.cardId
                repository.rateCard(
                    RateCardRequest("session-1", position, cardId, RatingDecision.GotIt, RatingInputSource.Gesture),
                    ratedAt.plusSeconds(position.toLong()),
                )
            }
            val lastPosition = 6
            val result =
                repository.rateCard(
                    RateCardRequest("session-1", lastPosition, "card-5", RatingDecision.GotIt, RatingInputSource.Gesture),
                    ratedAt.plusSeconds(6),
                )

            assertTrue(result is RateCardResult.Rated)
            assertTrue((result as RateCardResult.Rated).completed)
            assertEquals(7, result.completion?.totalCount)
            assertEquals("COMPLETED", database.sessionDao().get("session-1")?.status)
        }

    @Test
    fun `undo removes the runtime reinserted card and restores the original length`() =
        runTest {
            repository.rateCard(request(0, "card-0"), ratedAt)

            val undo = repository.undoLastRating(com.signkorean.domain.port.input.UndoRatingRequest("session-1"))

            assertTrue(undo is com.signkorean.domain.port.input.UndoRatingResult.Undone)
            assertEquals(6, database.sessionCardDao().countForSession("session-1"))
            assertEquals(6, database.sessionDao().get("session-1")?.requestedLength)
            assertEquals(listOf("card-0", "card-1", "card-2", "card-3", "card-4", "card-5"), database.sessionCardDao().forSession("session-1").map { it.cardId })
        }

    private fun request(position: Int, cardId: String) =
        RateCardRequest("session-1", position, cardId, RatingDecision.Review, RatingInputSource.Gesture)

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}
