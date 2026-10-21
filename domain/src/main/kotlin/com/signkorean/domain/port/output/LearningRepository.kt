package com.signkorean.domain.port.output

import com.signkorean.domain.model.CardReviewState
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import java.time.Instant

interface LearningRepository {
    fun getReviewState(cardId: String): CardReviewState

    suspend fun rateCard(
        request: RateCardRequest,
        ratedAt: Instant,
    ): RateCardResult = RateCardResult.Unavailable
}
