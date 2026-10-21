package com.signkorean.domain.usecase

import com.signkorean.domain.model.CardReport
import com.signkorean.domain.model.CardReportReason
import com.signkorean.domain.model.CardReportSyncState
import com.signkorean.domain.model.ReportDetails
import com.signkorean.domain.port.input.SubmitCardReportRequest
import com.signkorean.domain.port.input.SubmitCardReportResult
import com.signkorean.domain.port.output.CardReportEnqueueResult
import com.signkorean.domain.port.output.CardReportIdGenerator
import com.signkorean.domain.port.output.CardReportRepository
import com.signkorean.domain.port.output.ClockPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SubmitCardReportUseCaseTest {
    @Test
    fun `valid report is queued as local only without exposing details elsewhere`() =
        runTest {
            val repository = RecordingRepository(CardReportEnqueueResult.Enqueued)
            val useCase = SubmitCardReportUseCase(repository, fixedClock, fixedIds)

            val result =
                useCase(
                    SubmitCardReportRequest(
                        cardId = "card-1",
                        reason = CardReportReason.WRONG_MEANING,
                        details = ReportDetails("The meaning needs a review."),
                    ),
                )

            assertEquals(SubmitCardReportResult.Saved("report-1"), result)
            assertEquals(
                CardReport(
                    reportId = "report-1",
                    cardId = "card-1",
                    reason = CardReportReason.WRONG_MEANING,
                    details = ReportDetails("The meaning needs a review."),
                    createdAt = Instant.parse("2026-10-21T00:00:00Z"),
                    syncState = CardReportSyncState.LOCAL_ONLY,
                ),
                repository.enqueued,
            )
        }

    @Test
    fun `duplicate result is surfaced without pretending the report was newly saved`() =
        runTest {
            val result =
                SubmitCardReportUseCase(
                    RecordingRepository(CardReportEnqueueResult.Duplicate),
                    fixedClock,
                    fixedIds,
                )(
                    SubmitCardReportRequest(
                        "card-1",
                        CardReportReason.OTHER,
                        ReportDetails("Already sent"),
                    ),
                )

            assertEquals(SubmitCardReportResult.Duplicate, result)
        }

    @Test
    fun `storage failure is surfaced as unavailable`() =
        runTest {
            val result =
                SubmitCardReportUseCase(
                    RecordingRepository(CardReportEnqueueResult.Unavailable),
                    fixedClock,
                    fixedIds,
                )(
                    SubmitCardReportRequest(
                        "card-1",
                        CardReportReason.PHOTO_PROBLEM,
                        ReportDetails("The photo is blurry."),
                    ),
                )

            assertEquals(SubmitCardReportResult.Unavailable, result)
        }

    @Test
    fun `details are limited by unicode code points rather than utf-16 units`() {
        val emojiDetails = ReportDetails("😀".repeat(500))

        assertEquals(500, emojiDetails.value.codePointCount(0, emojiDetails.value.length))
        assertTrue(runCatching { ReportDetails("😀".repeat(501)) }.isFailure)
    }

    private class RecordingRepository(
        private val result: CardReportEnqueueResult,
    ) : CardReportRepository {
        var enqueued: CardReport? = null

        override suspend fun enqueue(report: CardReport): CardReportEnqueueResult {
            enqueued = report
            return result
        }

        override fun observePending(): Flow<List<CardReport>> = emptyFlow()
    }

    private companion object {
        val fixedClock =
            object : ClockPort {
                override fun now(): Instant = Instant.parse("2026-10-21T00:00:00Z")

                override fun zoneId(): ZoneId = ZoneId.of("UTC")
            }
        val fixedIds = CardReportIdGenerator { "report-1" }
    }
}
