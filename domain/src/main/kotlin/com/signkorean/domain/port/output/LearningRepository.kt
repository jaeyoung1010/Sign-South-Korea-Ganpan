package com.signkorean.domain.port.output

import com.signkorean.domain.model.CardReviewState
import com.signkorean.domain.model.CardProgress
import com.signkorean.domain.model.ProgressReview
import com.signkorean.domain.model.RatingActivity
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.input.UndoRatingRequest
import com.signkorean.domain.port.input.UndoRatingResult
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface LearningRepository {
    fun getReviewState(cardId: String): CardReviewState

    fun observeProgress(now: Instant): Flow<List<CardProgress>> = flowOf(emptyList())

    fun observeRatingActivity(): Flow<List<RatingActivity>> = flowOf(emptyList())

    fun observeReviewCards(
        limit: Int,
        offset: Int,
    ): Flow<List<ProgressReview>> = flowOf(emptyList())

    suspend fun rateCard(
        request: RateCardRequest,
        ratedAt: Instant,
    ): RateCardResult = RateCardResult.Unavailable

    suspend fun undoLastRating(request: UndoRatingRequest): UndoRatingResult = UndoRatingResult.Unavailable
}
