package com.signkorean.domain.model

data class ReviewCardDetail(
    val card: SignCard,
    val copy: LocalizedCardCopy,
) {
    init {
        require(card.id == copy.cardId) { "Review detail card and copy IDs must match" }
    }
}
