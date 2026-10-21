package com.signkorean.domain.model

import java.time.Instant
import java.time.LocalDate

data class ActiveSession(
    val completedCount: Int,
    val totalCount: Int,
    val sessionId: String = "",
    val isPaused: Boolean = false,
) {
    init {
        require(completedCount in 0..totalCount) { "Completed count must be within the session length" }
        require(totalCount > 0) { "Session length must be positive" }
    }
}

data class HomeActivitySnapshot(
    val progressedCardIds: Set<String> = emptySet(),
    val dueCardIds: Set<String> = emptySet(),
    val reviewedAt: List<Instant> = emptyList(),
    val dueCards: List<DueCard> = emptyList(),
)

data class DueCard(
    val cardId: String,
    val nextReviewAt: Instant,
    val reviewCount: Int,
) {
    init {
        require(cardId.isNotBlank()) { "Due card ID must not be blank" }
        require(reviewCount >= 0) { "Review count must not be negative" }
    }
}

data class LearnHomeDashboard(
    val streakDays: Int,
    val session: LearnHomeSession,
    val categories: List<LearnHomeCategory>,
    val weeklyActivity: List<LearnHomeDay>,
)

data class LearnHomeSession(
    val length: Int,
    val estimatedMinutes: Int,
    val reviewCount: Int,
    val newCount: Int,
    val activeSession: ActiveSession?,
)

data class LearnHomeCategory(
    val key: String,
    val label: String,
    val cardCount: Int,
)

data class LearnHomeDay(
    val date: LocalDate,
    val count: Int,
)
