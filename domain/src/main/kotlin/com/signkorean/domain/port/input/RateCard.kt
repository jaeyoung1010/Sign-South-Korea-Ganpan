package com.signkorean.domain.port.input

import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.RatingInputSource

fun interface RateCard {
    suspend operator fun invoke(request: RateCardRequest): RateCardResult
}

data class RateCardRequest(
    val sessionId: String,
    val position: Int,
    val cardId: String,
    val decision: RatingDecision,
    val source: RatingInputSource,
) {
    init {
        require(sessionId.isNotBlank())
        require(position >= 0)
        require(cardId.isNotBlank())
    }
}

sealed interface RateCardResult {
    data class Rated(
        val nextPosition: Int,
        val completed: Boolean,
    ) : RateCardResult

    data object AlreadyRated : RateCardResult

    data object Unavailable : RateCardResult

    data object Failed : RateCardResult
}
