package com.signkorean.domain.model

import com.signkorean.domain.gesture.RatingDecision

data class CardRatedEvent(
    val cardId: String,
    val position: Int,
    val decision: RatingDecision,
    val source: RatingInputSource,
) {
    init {
        require(cardId.isNotBlank())
        require(position > 0)
    }
}

enum class RatingInputSource {
    Gesture,
    Keyboard,
    Accessibility,
}
