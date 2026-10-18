package com.signkorean.domain.port.output

import com.signkorean.domain.model.CardReviewState

interface LearningRepository {
    fun getReviewState(cardId: String): CardReviewState
}
