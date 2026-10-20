package com.signkorean.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.signkorean.domain.model.AppContent
import com.signkorean.testsupport.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomContentMigrationRepositoryTest {
    private lateinit var database: SignKoreanDatabase
    private lateinit var repository: RoomContentMigrationRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, SignKoreanDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomContentMigrationRepository(database, Dispatchers.Unconfined, FakeClock())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `new content version keeps valid progress and abandons invalid active session`() =
        runTest {
            database.contentStateDao().upsert(ContentStateEntity(contentVersion = 1))
            database.cardProgressDao().upsert(CardProgressEntity.initial("known-card"))
            database.cardProgressDao().upsert(CardProgressEntity.initial("removed-card"))
            database.sessionDao().insert(SessionEntity.active(id = "active-session", requestedLength = 2))
            database.sessionCardDao().insertAll(
                listOf(
                    SessionCardEntity("active-session", 0, "known-card", "NEW"),
                    SessionCardEntity("active-session", 1, "removed-card", "NEW"),
                ),
            )

            val result = repository.migrate(AppContent(version = 2, cardIds = setOf("known-card")))

            assertTrue(result.migrated)
            assertTrue(result.activeSessionInvalidated)
            assertEquals("ABANDONED", database.sessionDao().get("active-session")?.status)
            assertEquals("known-card", database.cardProgressDao().get("known-card")?.cardId)
            assertNull(database.cardProgressDao().get("removed-card"))
            assertEquals(2, database.contentStateDao().current()?.contentVersion)
        }

    @Test
    fun `same content version does not rewrite active session`() =
        runTest {
            repository.migrate(AppContent(version = 2, cardIds = setOf("known-card")))
            database.sessionDao().insert(SessionEntity.active(id = "active-session", requestedLength = 1))
            database.sessionCardDao().insertAll(listOf(SessionCardEntity("active-session", 0, "known-card", "NEW")))

            val result = repository.migrate(AppContent(version = 2, cardIds = setOf("known-card")))

            assertFalse(result.migrated)
            assertFalse(result.activeSessionInvalidated)
            assertEquals("ACTIVE", database.sessionDao().get("active-session")?.status)
        }

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}
