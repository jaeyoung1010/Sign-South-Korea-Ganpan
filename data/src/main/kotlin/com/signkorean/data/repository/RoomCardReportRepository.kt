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
        database.cardReportDao().observePending().map { reports -> reports.map(CardReportEntity::toDomain) }
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

private fun CardReportEntity.toDomain(): CardReport =
    CardReport(
        reportId = reportId,
        cardId = cardId,
        reason = CardReportReason.entries.first { it.storageValue == reason },
        details = ReportDetails(details),
        createdAt = createdAt,
        syncState = CardReportSyncState.entries.first { it.storageValue == syncState },
    )
