package com.signkorean.data.repository

import com.signkorean.domain.model.CardReviewState
import com.signkorean.domain.port.output.LearningRepository

class EmptyLearningRepository : LearningRepository {
    override fun getReviewState(cardId: String): CardReviewState = CardReviewState.NotFound
}
