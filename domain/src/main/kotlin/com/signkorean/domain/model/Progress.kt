package com.signkorean.domain.model

import com.signkorean.domain.gesture.RatingDecision
import java.time.Instant
import java.time.LocalDate

data class ProgressSnapshot(
    val learnedCount: Int,
    val masteredCount: Int,
    val dueCount: Int,
    val weeklyActivity: List<ProgressDay>,
    val categoryProgress: List<ProgressCategory>,
    val recentReviews: List<ProgressReview>,
) {
    init {
        require(learnedCount >= 0)
        require(masteredCount in 0..learnedCount)
        require(dueCount >= 0)
    }
}

data class ProgressDay(
    val date: LocalDate,
    val count: Int,
) {
    init {
        require(count >= 0)
    }
}

data class ProgressCategory(
    val key: String,
    val learnedCount: Int,
    val totalCount: Int,
    val ratio: Float,
) {
    init {
        require(key.isNotBlank())
        require(learnedCount in 0..totalCount)
        require(totalCount > 0)
        require(ratio in 0f..1f)
    }
}

data class ProgressReview(
    val cardId: String,
    val reviewedAt: Instant,
) {
    init {
        require(cardId.isNotBlank())
    }
}

data class RatingActivity(
    val cardId: String,
    val decision: RatingDecision,
    val ratedAt: Instant,
) {
    init {
        require(cardId.isNotBlank())
    }
}
