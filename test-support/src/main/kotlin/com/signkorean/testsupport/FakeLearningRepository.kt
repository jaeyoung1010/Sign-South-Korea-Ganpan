package com.signkorean.testsupport

import com.signkorean.domain.model.CardReviewState
import com.signkorean.domain.port.output.LearningRepository

class FakeLearningRepository : LearningRepository {
    private val reviewStates = mutableMapOf<String, CardReviewState>()

    override fun getReviewState(cardId: String): CardReviewState = reviewStates[cardId] ?: CardReviewState.NotFound

    fun setReviewState(
        cardId: String,
        state: CardReviewState,
    ) {
        reviewStates[cardId] = state
    }
}
