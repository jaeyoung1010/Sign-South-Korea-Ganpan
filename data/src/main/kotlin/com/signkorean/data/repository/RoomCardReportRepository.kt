package com.signkorean.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.signkorean.data.db.CardReportEntity
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.model.CardReport
import com.signkorean.domain.model.CardReportReason
import com.signkorean.domain.model.CardReportSyncState
import com.signkorean.domain.model.ReportDetails
import com.signkorean.domain.port.output.CardReportEnqueueResult
import com.signkorean.domain.port.output.CardReportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomCardReportRepository(
    private val database: SignKoreanDatabase,
    private val dispatcher: CoroutineDispatcher,
) : CardReportRepository {
    override suspend fun enqueue(report: CardReport): CardReportEnqueueResult =
        withContext(dispatcher) {
            try {
                database.cardReportDao().insert(report.toEntity())
                CardReportEnqueueResult.Enqueued
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: SQLiteConstraintException) {
                if (database.cardReportDao().count(report.reportId) > 0) {
                    CardReportEnqueueResult.Duplicate
                } else {
                    CardReportEnqueueResult.Unavailable
                }
            } catch (_: Exception) {
                CardReportEnqueueResult.Unavailable
            }
        }

    override fun observePending(): Flow<List<CardReport>> =
        database.cardReportDao().observePending().map { reports -> reports.mapNotNull(CardReportEntity::toDomainOrNull) }
}

private fun CardReport.toEntity(): CardReportEntity =
    CardReportEntity(
        reportId = reportId,
        cardId = cardId,
        reason = reason.storageValue,
        details = details.value,
        createdAt = createdAt,
        syncState = syncState.storageValue,
    )

private fun CardReportEntity.toDomainOrNull(): CardReport? {
    val parsedReason = CardReportReason.entries.firstOrNull { it.storageValue == reason } ?: return null
    val parsedSyncState = CardReportSyncState.entries.firstOrNull { it.storageValue == syncState } ?: return null
    return runCatching {
        CardReport(
            reportId = reportId,
            cardId = cardId,
            reason = parsedReason,
            details = ReportDetails(details),
            createdAt = createdAt,
            syncState = parsedSyncState,
        )
    }.getOrNull()
}
