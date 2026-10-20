package com.signkorean.domain.session

import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.UserSettings

/** The information needed to select a card without coupling selection to persistence. */
data class SessionCandidate(
    val cardId: String,
    val order: Int,
    val category: String,
    val isDue: Boolean,
    val isNew: Boolean,
) {
    init {
        require(cardId.isNotBlank()) { "Card ID must not be blank" }
        require(category.isNotBlank()) { "Category must not be blank" }
    }
}

enum class SessionType {
    DAILY,
    CATEGORY,
    PRACTICE,
}

/**
 * A complete, persistence-free selection request. The seed is kept with a session by a later
 * adapter so the same request can always be rebuilt in the same order.
 */
data class SessionBuildRequest(
    val type: SessionType,
    val length: Int,
    val candidates: List<SessionCandidate>,
    val interests: Set<Interest> = emptySet(),
    val level: LearningLevel = LearningLevel.BEGINNER,
    val category: String? = null,
    val practiceCardId: String? = null,
    val seed: Long,
) {
    init {
        when (type) {
            SessionType.DAILY -> {
                require(length in UserSettings.SESSION_LENGTH_OPTIONS)
            }

            SessionType.CATEGORY -> {
                require(length in UserSettings.SESSION_LENGTH_OPTIONS)
                require(!category.isNullOrBlank()) { "Category sessions need a category" }
            }

            SessionType.PRACTICE -> {
                require(length == 1) { "Practice sessions contain one card" }
                require(!practiceCardId.isNullOrBlank()) { "Practice sessions need a card" }
            }
        }
    }

    companion object {
        fun daily(
            length: Int,
            candidates: List<SessionCandidate>,
            interests: Set<Interest>,
            level: LearningLevel,
            seed: Long,
        ): SessionBuildRequest =
            SessionBuildRequest(
                type = SessionType.DAILY,
                length = length,
                candidates = candidates,
                interests = interests,
                level = level,
                seed = seed,
            )

        fun category(
            length: Int,
            category: String,
            candidates: List<SessionCandidate>,
            interests: Set<Interest> = emptySet(),
            level: LearningLevel = LearningLevel.BEGINNER,
            seed: Long,
        ): SessionBuildRequest =
            SessionBuildRequest(
                type = SessionType.CATEGORY,
                length = length,
                candidates = candidates,
                interests = interests,
                level = level,
                category = category,
                seed = seed,
            )

        fun practice(
            cardId: String,
            candidates: List<SessionCandidate>,
            seed: Long,
        ): SessionBuildRequest =
            SessionBuildRequest(
                type = SessionType.PRACTICE,
                length = 1,
                candidates = candidates,
                practiceCardId = cardId,
                seed = seed,
            )
    }
}

/** A later review phase can replace this hook without changing session selection. */
enum class ReviewReinsertionPolicy {
    DEFERRED,
}

data class SessionPlan(
    val cardIds: List<String>,
    val reviewReinsertionPolicy: ReviewReinsertionPolicy = ReviewReinsertionPolicy.DEFERRED,
)
