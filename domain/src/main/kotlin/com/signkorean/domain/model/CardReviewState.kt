package com.signkorean.domain.model

sealed interface CardReviewState {
    data object Due : CardReviewState

    data object NotFound : CardReviewState
}
