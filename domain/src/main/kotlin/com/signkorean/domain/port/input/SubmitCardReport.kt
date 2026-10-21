package com.signkorean.domain.port.input

import com.signkorean.domain.model.CardReportReason
import com.signkorean.domain.model.ReportDetails

fun interface SubmitCardReport {
    suspend operator fun invoke(request: SubmitCardReportRequest): SubmitCardReportResult
}

data class SubmitCardReportRequest(
    val cardId: String,
    val reason: CardReportReason,
    val details: ReportDetails,
) {
    init {
        require(cardId.isNotBlank())
    }
}

sealed interface SubmitCardReportResult {
    data class Saved(
        val reportId: String,
    ) : SubmitCardReportResult

    data object Duplicate : SubmitCardReportResult

    data object Unavailable : SubmitCardReportResult
}
