package com.signkorean.domain.port.output

import com.signkorean.domain.model.CardReviewState
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.input.UndoRatingRequest
import com.signkorean.domain.port.input.UndoRatingResult
import java.time.Instant

interface LearningRepository {
    fun getReviewState(cardId: String): CardReviewState

    suspend fun rateCard(
        request: RateCardRequest,
        ratedAt: Instant,
    ): RateCardResult = RateCardResult.Unavailable

    suspend fun undoLastRating(request: UndoRatingRequest): UndoRatingResult = UndoRatingResult.Unavailable
}
