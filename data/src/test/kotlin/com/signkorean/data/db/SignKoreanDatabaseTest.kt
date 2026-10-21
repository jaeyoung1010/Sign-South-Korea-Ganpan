package com.signkorean.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.TypeConverters
import androidx.test.core.app.ApplicationProvider
import com.signkorean.testsupport.FakeClock
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SignKoreanDatabaseTest {
    private lateinit var database: SignKoreanDatabase
    private lateinit var clock: FakeClock
    private lateinit var transactionStore: RoomSessionTransactionStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, SignKoreanDatabase::class.java).allowMainThreadQueries().build()
        clock = FakeClock(at(300))
        transactionStore = RoomSessionTransactionStore(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `stores progress session cards snapshot and report with their constraints`() {
        database.sessionDao().insert(SessionEntity.active(id = "session-1", requestedLength = 1))
        database.sessionCardDao().insertAll(
            listOf(SessionCardEntity(sessionId = "session-1", position = 0, cardId = "01-hongdae-entrance", source = "NEW")),
        )
        database.cardProgressDao().upsert(CardProgressEntity.initial("01-hongdae-entrance"))
        database.ratingSnapshotDao().upsert(
            RatingSnapshotEntity.noProgress(sessionId = "session-1", cardId = "01-hongdae-entrance", position = 0),
        )
        database.cardReportDao().insert(
            CardReportEntity(
                reportId = "report-1",
                cardId = "01-hongdae-entrance",
                reason = "WRONG_MEANING",
                details = "Needs review",
                createdAt = at(10),
                syncState = "LOCAL_ONLY",
            ),
        )

        assertEquals(1, database.cardProgressDao().count())
        assertEquals(1, database.sessionCardDao().forSession("session-1").size)
        assertEquals("01-hongdae-entrance", database.ratingSnapshotDao().forSession("session-1")?.cardId)
        assertEquals(1, database.cardReportDao().count())
    }

    @Test
    fun `session cards require an existing session and cascade on session deletion`() {
        val orphanFailure =
            runCatching {
                database.sessionCardDao().insertAll(
                    listOf(SessionCardEntity("missing", 0, "01-hongdae-entrance", "NEW")),
                )
            }.exceptionOrNull()
        assertTrue(orphanFailure != null)

        database.sessionDao().insert(SessionEntity.active(id = "session-1", requestedLength = 1))
        database.sessionCardDao().insertAll(
            listOf(SessionCardEntity("session-1", 0, "01-hongdae-entrance", "NEW")),
        )
        database.openHelper.writableDatabase.execSQL("DELETE FROM sessions WHERE session_id = 'session-1'")

        assertEquals(0, database.sessionCardDao().countForSession("session-1"))
    }

    @Test
    fun `session creation writes its cards in the same transaction`() {
        transactionStore.createSession(
            SessionCreationCommand(
                session = SessionEntity.active(id = "created-session", requestedLength = 2),
                cards =
                    listOf(
                        SessionCardEntity("created-session", 0, "01-hongdae-entrance", "NEW"),
                        SessionCardEntity("created-session", 1, "02-cafe", "NEW"),
                    ),
            ),
        )

        assertEquals("ACTIVE", database.sessionDao().get("created-session")?.status)
        assertEquals(2, database.sessionCardDao().countForSession("created-session"))
    }

    @Test
    fun `session creation rolls back when card insertion fails`() {
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_session_card_insert BEFORE INSERT ON session_cards
            WHEN NEW.session_id = 'rollback-session' BEGIN SELECT RAISE(ABORT, 'stop'); END
            """.trimIndent(),
        )

        val failure =
            runCatching {
                transactionStore.createSession(
                    SessionCreationCommand(
                        session = SessionEntity.active(id = "rollback-session", requestedLength = 1),
                        cards = listOf(SessionCardEntity("rollback-session", 0, "01-hongdae-entrance", "NEW")),
                    ),
                )
            }.exceptionOrNull()

        assertTrue(failure != null)
        assertNull(database.sessionDao().get("rollback-session"))
        assertEquals(0, database.sessionCardDao().countForSession("rollback-session"))
    }

    @Test
    fun `session creation preserves an existing active session`() {
        database.sessionDao().insert(SessionEntity.active(id = "existing-session", requestedLength = 1))
        database.sessionCardDao().insertAll(
            listOf(SessionCardEntity("existing-session", 0, "01-hongdae-entrance", "NEW")),
        )

        val failure =
            runCatching {
                transactionStore.createSession(
                    SessionCreationCommand(
                        session = SessionEntity.active(id = "new-session", requestedLength = 1),
                        cards = listOf(SessionCardEntity("new-session", 0, "01-hongdae-entrance", "NEW")),
                    ),
                )
            }.exceptionOrNull()

        assertTrue(failure != null)
        assertEquals("existing-session", database.sessionDao().active()?.sessionId)
        assertNull(database.sessionDao().get("new-session"))
    }

    @Test
    fun `rating updates progress card and session as one transaction`() {
        seedActiveSession()

        transactionStore.rate(
            RatingCommand(
                sessionId = "session-1",
                position = 0,
                cardId = "01-hongdae-entrance",
                rating = "GOT_IT",
                ratedAt = at(100),
                updatedProgress = progress("GOT_IT"),
            ),
        )

        assertEquals(1, database.cardProgressDao().get("01-hongdae-entrance")?.stage)
        assertEquals("GOT_IT", database.sessionCardDao().get("session-1", 0)?.rating)
        assertEquals(1, database.sessionDao().get("session-1")?.currentIndex)
        assertEquals("COMPLETED", database.sessionDao().get("session-1")?.status)
    }

    @Test
    fun `rating rollback leaves no partial state when final session update fails`() {
        seedActiveSession()
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_session_advance BEFORE UPDATE ON sessions
            WHEN NEW.current_index = 1 BEGIN SELECT RAISE(ABORT, 'stop'); END
            """.trimIndent(),
        )

        val failure =
            runCatching {
                transactionStore.rate(
                    RatingCommand(
                        sessionId = "session-1",
                        position = 0,
                        cardId = "01-hongdae-entrance",
                        rating = "REVIEW",
                        ratedAt = at(100),
                        updatedProgress = CardProgressEntity.initial("01-hongdae-entrance"),
                    ),
                )
            }.exceptionOrNull()

        assertTrue(failure != null)
        assertNull(database.cardProgressDao().get("01-hongdae-entrance"))
        assertNull(database.sessionCardDao().get("session-1", 0)?.rating)
        assertEquals(0, database.sessionDao().get("session-1")?.currentIndex)
        assertNull(database.ratingSnapshotDao().forSession("session-1"))
    }

    @Test
    fun `undo restores the previous progress and makes the session active again`() {
        seedActiveSession()
        transactionStore.rate(
            RatingCommand("session-1", 0, "01-hongdae-entrance", "GOT_IT", at(100), progress("GOT_IT")),
        )

        transactionStore.undoLastRating("session-1")

        assertNull(database.cardProgressDao().get("01-hongdae-entrance"))
        assertNull(database.sessionCardDao().get("session-1", 0)?.rating)
        assertEquals(0, database.sessionDao().get("session-1")?.currentIndex)
        assertEquals("ACTIVE", database.sessionDao().get("session-1")?.status)
        assertEquals(at(300), database.sessionDao().get("session-1")?.updatedAt)
    }

    @Test
    fun `time converters preserve instant and local date values`() {
        val converters = RoomTimeConverters()
        assertEquals(at(123), converters.epochMillisToInstant(converters.instantToEpochMillis(at(123))))
        assertEquals(
            LocalDate.of(2026, 10, 19),
            converters.isoStringToLocalDate(converters.localDateToIsoString(LocalDate.of(2026, 10, 19))),
        )
    }

    @Test
    fun `file backed database preserves an active session after reopening`() {
        val databaseName = "phase-7-file-backed.db"
        context.deleteDatabase(databaseName)
        val first = Room.databaseBuilder(context, SignKoreanDatabase::class.java, databaseName).allowMainThreadQueries().build()
        first.sessionDao().insert(SessionEntity.active(id = "persistent-session", requestedLength = 5))
        first.close()

        val reopened = Room.databaseBuilder(context, SignKoreanDatabase::class.java, databaseName).allowMainThreadQueries().build()
        try {
            assertEquals("ACTIVE", reopened.sessionDao().active()?.status)
        } finally {
            reopened.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun `migrates a v1 database to v3 without losing sessions`() {
        val databaseName = "phase-7-v1-to-v2.db"
        context.deleteDatabase(databaseName)
        val v1 = Room.databaseBuilder(context, LegacyV1Database::class.java, databaseName).allowMainThreadQueries().build()
        v1.sessionDao().insert(SessionEntity.active(id = "v1-session", requestedLength = 3))
        v1.close()

        val v2 =
            Room
                .databaseBuilder(context, SignKoreanDatabase::class.java, databaseName)
                .addMigrations(SignKoreanDatabase.MIGRATION_1_2)
                .addMigrations(SignKoreanDatabase.MIGRATION_2_3)
                .allowMainThreadQueries()
                .build()
        try {
            assertEquals("ACTIVE", v2.sessionDao().get("v1-session")?.status)
            v2.ratingSnapshotDao().upsert(RatingSnapshotEntity.noProgress("v1-session", "card", 0))
            assertEquals("card", v2.ratingSnapshotDao().forSession("v1-session")?.cardId)
        } finally {
            v2.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun seedActiveSession() {
        database.sessionDao().insert(SessionEntity.active(id = "session-1", requestedLength = 1))
        database.sessionCardDao().insertAll(
            listOf(SessionCardEntity(sessionId = "session-1", position = 0, cardId = "01-hongdae-entrance", source = "NEW")),
        )
    }

    private fun progress(rating: String) =
        CardProgressEntity(
            cardId = "01-hongdae-entrance",
            stage = 1,
            mastered = false,
            lastRating = rating,
            lastReviewedAt = at(100),
            nextReviewAt = at(200),
            reviewCount = 1,
            gotItCount = if (rating == "GOT_IT") 1 else 0,
            version = 1,
        )

    private fun at(millis: Long): Instant = Instant.ofEpochMilli(millis)

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}

@Database(
    entities = [CardProgressEntity::class, SessionEntity::class, SessionCardEntity::class, CardReportEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(RoomTimeConverters::class)
abstract class LegacyV1Database : androidx.room.RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
