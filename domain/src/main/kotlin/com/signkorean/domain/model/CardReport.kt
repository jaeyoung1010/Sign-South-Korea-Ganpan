package com.signkorean.domain.model

enum class CardReportReason(
    val storageValue: String,
) {
    WRONG_KOREAN("WRONG_KOREAN"),
    WRONG_MEANING("WRONG_MEANING"),
    PHOTO_PROBLEM("PHOTO_PROBLEM"),
    AUDIO_PROBLEM("AUDIO_PROBLEM"),
    OTHER("OTHER"),
}

enum class CardReportSyncState(
    val storageValue: String,
) {
    LOCAL_ONLY("LOCAL_ONLY"),
    PENDING("PENDING"),
    SENT("SENT"),
    FAILED("FAILED"),
}

data class ReportDetails(
    val value: String,
) {
    init {
        require(value.codePointCount(0, value.length) <= MAX_CODE_POINTS) {
            "Report details must be at most $MAX_CODE_POINTS Unicode code points"
        }
    }

    companion object {
        const val MAX_CODE_POINTS = 500
    }
}

data class CardReport(
    val reportId: String,
    val cardId: String,
    val reason: CardReportReason,
    val details: ReportDetails,
    val createdAt: java.time.Instant,
    val syncState: CardReportSyncState,
) {
    init {
        require(reportId.isNotBlank())
        require(cardId.isNotBlank())
    }
}
