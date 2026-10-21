package com.signkorean.domain.model

import com.signkorean.domain.gesture.RatingDecision

data class CardRatingUndoneEvent(
    val cardId: String,
    val previousRating: RatingDecision,
) {
    init {
        require(cardId.isNotBlank())
    }
}
