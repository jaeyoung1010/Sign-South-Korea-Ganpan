package com.signkorean.domain.usecase

import com.signkorean.domain.model.CardReport
import com.signkorean.domain.model.CardReportSyncState
import com.signkorean.domain.port.input.SubmitCardReport
import com.signkorean.domain.port.input.SubmitCardReportRequest
import com.signkorean.domain.port.input.SubmitCardReportResult
import com.signkorean.domain.port.output.CardReportIdGenerator
import com.signkorean.domain.port.output.CardReportRepository
import com.signkorean.domain.port.output.ClockPort

class SubmitCardReportUseCase(
    private val cardReportRepository: CardReportRepository,
    private val clock: ClockPort,
    private val idGenerator: CardReportIdGenerator,
) : SubmitCardReport {
    override suspend fun invoke(request: SubmitCardReportRequest): SubmitCardReportResult {
        val report =
            CardReport(
                reportId = idGenerator.nextId(),
                cardId = request.cardId,
                reason = request.reason,
                details = request.details,
                createdAt = clock.now(),
                syncState = CardReportSyncState.LOCAL_ONLY,
            )
        return when (cardReportRepository.enqueue(report)) {
            com.signkorean.domain.port.output.CardReportEnqueueResult.Enqueued -> {
                SubmitCardReportResult.Saved(report.reportId)
            }

            com.signkorean.domain.port.output.CardReportEnqueueResult.Duplicate -> {
                SubmitCardReportResult.Duplicate
            }

            com.signkorean.domain.port.output.CardReportEnqueueResult.Unavailable -> {
                SubmitCardReportResult.Unavailable
            }
        }
    }
}
