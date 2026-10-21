package com.signkorean.domain.port.output

import com.signkorean.domain.model.CardReport
import kotlinx.coroutines.flow.Flow

interface CardReportRepository {
    suspend fun enqueue(report: CardReport): CardReportEnqueueResult

    fun observePending(): Flow<List<CardReport>>
}

enum class CardReportEnqueueResult {
    Enqueued,
    Duplicate,
    Unavailable,
}
