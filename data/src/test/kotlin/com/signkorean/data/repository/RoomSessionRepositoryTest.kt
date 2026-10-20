package com.signkorean.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.signkorean.data.db.SessionCardEntity
import com.signkorean.data.db.SessionEntity
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.model.AnswerRevealWriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomSessionRepositoryTest {
    private lateinit var database: SignKoreanDatabase
    private lateinit var repository: RoomSessionRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, SignKoreanDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomSessionRepository(database, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `loads the active index and ordered card snapshot together`() =
        runTest {
            database.sessionDao().insert(SessionEntity.active(id = "active", requestedLength = 3).copy(currentIndex = 1))
            database
                .sessionCardDao()
                .insertAll(
                    listOf(
                        sessionCard(0, "card-1"),
                        sessionCard(1, "card-2"),
                        sessionCard(2, "card-3"),
                    ),
                )

            val snapshot = repository.getActiveLearnSession()

            assertNotNull(snapshot)
            assertEquals("active", snapshot?.sessionId)
            assertEquals(1, snapshot?.currentIndex)
            assertEquals(listOf("card-1", "card-2", "card-3"), snapshot?.cards?.map { it.cardId })
        }

    @Test
    fun `records an answer reveal once for the current active card`() =
        runTest {
            database.sessionDao().insert(SessionEntity.active(id = "active", requestedLength = 1))
            database.sessionCardDao().insertAll(listOf(sessionCard(0, "card-1")))
            val revealedAt = Instant.parse("2026-10-20T00:00:00Z")

            val first = repository.markAnswerRevealed("active", 0, "card-1", revealedAt)
            val duplicate = repository.markAnswerRevealed("active", 0, "card-1", revealedAt.plusSeconds(1))

            assertEquals(AnswerRevealWriteResult.Recorded, first)
            assertEquals(AnswerRevealWriteResult.AlreadyRecorded, duplicate)
            assertEquals(revealedAt, database.sessionCardDao().get("active", 0)?.answerRevealedAt)
        }

    private fun sessionCard(
        position: Int,
        cardId: String,
    ) = SessionCardEntity(
        sessionId = "active",
        position = position,
        cardId = cardId,
        source = "NEW",
    )

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}
