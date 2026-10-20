package com.signkorean.domain.port.input

import com.signkorean.domain.model.ActiveSession
import com.signkorean.domain.session.SessionType

fun interface StartSession {
    suspend operator fun invoke(request: StartSessionRequest): StartSessionResult
}

data class StartSessionRequest(
    val type: SessionType,
    val category: String? = null,
    val practiceCardId: String? = null,
    val replaceActiveSession: Boolean = false,
) {
    init {
        when (type) {
            SessionType.DAILY -> {
                require(category == null && practiceCardId == null)
            }

            SessionType.CATEGORY -> {
                require(!category.isNullOrBlank()) { "Category sessions need a category" }
                require(practiceCardId == null)
            }

            SessionType.PRACTICE -> {
                require(!practiceCardId.isNullOrBlank()) { "Practice sessions need a card" }
                require(category == null)
            }
        }
    }

    companion object {
        fun daily(): StartSessionRequest = StartSessionRequest(SessionType.DAILY)

        fun category(
            category: String,
            replaceActiveSession: Boolean = false,
        ): StartSessionRequest =
            StartSessionRequest(
                type = SessionType.CATEGORY,
                category = category,
                replaceActiveSession = replaceActiveSession,
            )

        fun practice(cardId: String): StartSessionRequest = StartSessionRequest(type = SessionType.PRACTICE, practiceCardId = cardId)
    }
}

sealed interface StartSessionResult {
    data class Started(
        val sessionId: String,
    ) : StartSessionResult

    data class Resumed(
        val activeSession: ActiveSession,
    ) : StartSessionResult

    data class ReplacementRequired(
        val activeSession: ActiveSession,
    ) : StartSessionResult

    data object NoCardsAvailable : StartSessionResult
}
