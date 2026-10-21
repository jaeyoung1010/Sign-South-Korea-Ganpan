package com.signkorean.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.model.CardReport
import com.signkorean.domain.model.CardReportReason
import com.signkorean.domain.model.CardReportSyncState
import com.signkorean.domain.model.ReportDetails
import com.signkorean.domain.port.output.CardReportEnqueueResult
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
class RoomCardReportRepositoryTest {
    private lateinit var database: SignKoreanDatabase
    private lateinit var repository: RoomCardReportRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, SignKoreanDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomCardReportRepository(database, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `enqueue stores a local-only report and pending flow exposes it`() =
        runTest {
            val result = repository.enqueue(report())

            assertEquals(CardReportEnqueueResult.Enqueued, result)
            assertEquals(listOf(report()), repository.observePending().first())
        }

    @Test
    fun `primary key collision is reported as duplicate`() =
        runTest {
            repository.enqueue(report())

            assertEquals(CardReportEnqueueResult.Duplicate, repository.enqueue(report()))
        }

    @Test
    fun `room insertion failure is reported as unavailable`() =
        runTest {
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_card_report_insert BEFORE INSERT ON card_reports " +
                    "BEGIN SELECT RAISE(ABORT, 'stop'); END",
            )

            assertEquals(CardReportEnqueueResult.Unavailable, repository.enqueue(report("report-failed")))
        }

    private fun report(reportId: String = "report-1") =
        CardReport(
            reportId = reportId,
            cardId = "card-1",
            reason = CardReportReason.WRONG_MEANING,
            details = ReportDetails("The meaning needs a review."),
            createdAt = Instant.parse("2026-10-21T00:00:00Z"),
            syncState = CardReportSyncState.LOCAL_ONLY,
        )

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}
