package com.signkorean.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.signkorean.data.db.SessionEntity
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.model.SessionCardSource
import com.signkorean.domain.model.SessionCardToCreate
import com.signkorean.domain.model.SessionToCreate
import com.signkorean.domain.session.SessionType
import com.signkorean.testsupport.FakeClock
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
class RoomSessionCreationRepositoryTest {
    private lateinit var database: SignKoreanDatabase
    private lateinit var repository: RoomSessionCreationRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, SignKoreanDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomSessionCreationRepository(database, FakeClock(at(50)), kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `writes active session and all ordered cards in one durable snapshot`() =
        runTest {
            repository.save(session("fresh"), replaceActiveSession = false)

            assertEquals("ACTIVE", database.sessionDao().get("fresh")?.status)
            assertEquals("CATEGORY", database.sessionDao().get("fresh")?.source)
            assertEquals("MARKET", database.sessionDao().get("fresh")?.category)
            assertEquals(
                listOf("REVIEW", "NEW"),
                database.sessionCardDao().forSession("fresh").map { it.source },
            )
        }

    @Test
    fun `confirmed category replacement abandons old active session and keeps the new cards`() =
        runTest {
            database.sessionDao().insert(SessionEntity.active(id = "old", requestedLength = 1))

            repository.save(session("replacement"), replaceActiveSession = true)

            assertEquals("ABANDONED", database.sessionDao().get("old")?.status)
            assertEquals("replacement", database.sessionDao().active()?.sessionId)
            assertEquals(2, database.sessionCardDao().countForSession("replacement"))
        }

    @Test
    fun `session card snapshot is available after reopening the database`() =
        runTest {
            val databaseName = "phase-15-session-recovery.db"
            context.deleteDatabase(databaseName)
            val first = Room.databaseBuilder(context, SignKoreanDatabase::class.java, databaseName).allowMainThreadQueries().build()
            try {
                RoomSessionCreationRepository(first, FakeClock(at(50)), kotlinx.coroutines.Dispatchers.Unconfined)
                    .save(session("persistent"), replaceActiveSession = false)
            } finally {
                first.close()
            }

            val reopened = Room.databaseBuilder(context, SignKoreanDatabase::class.java, databaseName).allowMainThreadQueries().build()
            try {
                assertEquals("persistent", reopened.sessionDao().active()?.sessionId)
                assertEquals(listOf("card-1", "card-2"), reopened.sessionCardDao().forSession("persistent").map { it.cardId })
            } finally {
                reopened.close()
                context.deleteDatabase(databaseName)
            }
        }

    private fun session(id: String) =
        SessionToCreate(
            id = id,
            requestedLength = 2,
            source = SessionType.CATEGORY,
            category = "MARKET",
            createdAt = at(100),
            cards =
                listOf(
                    SessionCardToCreate(0, "card-1", SessionCardSource.REVIEW),
                    SessionCardToCreate(1, "card-2", SessionCardSource.NEW),
                ),
        )

    private fun at(millis: Long): Instant = Instant.ofEpochMilli(millis)

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}
