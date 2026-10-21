package com.signkorean.domain.port.input

import com.signkorean.domain.gesture.RatingDecision

fun interface UndoRating {
    suspend operator fun invoke(request: UndoRatingRequest): UndoRatingResult
}

data class UndoRatingRequest(
    val sessionId: String,
) {
    init {
        require(sessionId.isNotBlank())
    }
}

sealed interface UndoRatingResult {
    data class Undone(
        val cardId: String,
        val position: Int,
        val rating: RatingDecision,
    ) : UndoRatingResult {
        init {
            require(cardId.isNotBlank())
            require(position >= 0)
        }
    }

    data object NothingToUndo : UndoRatingResult

    data object Unavailable : UndoRatingResult

    data object Failed : UndoRatingResult
}
