package com.signkorean.domain.port.input

import com.signkorean.domain.model.ReviewCardDetail

fun interface LoadReviewCardDetail {
    operator fun invoke(request: ReviewCardDetailRequest): ReviewCardDetail?
}

data class ReviewCardDetailRequest(
    val cardId: String,
    val languageTag: String = DEFAULT_LANGUAGE_TAG,
) {
    init {
        require(cardId.isNotBlank()) { "Review card ID must not be blank" }
        require(languageTag.isNotBlank()) { "Language tag must not be blank" }
    }

    private companion object {
        const val DEFAULT_LANGUAGE_TAG = "en"
    }
}
