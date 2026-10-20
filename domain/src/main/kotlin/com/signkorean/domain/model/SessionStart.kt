package com.signkorean.domain.model

import com.signkorean.domain.session.SessionType
import java.time.Instant

data class SessionToCreate(
    val id: String,
    val requestedLength: Int,
    val source: SessionType,
    val category: String?,
    val createdAt: Instant,
    val cards: List<SessionCardToCreate>,
) {
    init {
        require(id.isNotBlank()) { "Session ID must not be blank" }
        require(requestedLength == cards.size) { "Requested length must match the selected cards" }
        require(cards.isNotEmpty()) { "A session must contain at least one card" }
        require(cards.map(SessionCardToCreate::cardId).distinct().size == cards.size) { "Session cards must be unique" }
        require(category != null || source != SessionType.CATEGORY) { "Category sessions need a category" }
    }
}

data class SessionCardToCreate(
    val position: Int,
    val cardId: String,
    val source: SessionCardSource,
) {
    init {
        require(position >= 0) { "Session card position must not be negative" }
        require(cardId.isNotBlank()) { "Card ID must not be blank" }
    }
}

enum class SessionCardSource {
    REVIEW,
    NEW,
}

data class SessionStartedEvent(
    val source: SessionType,
    val dueCount: Int,
    val newCount: Int,
    val category: String?,
) {
    init {
        require(dueCount >= 0) { "Due count must not be negative" }
        require(newCount >= 0) { "New count must not be negative" }
    }
}
