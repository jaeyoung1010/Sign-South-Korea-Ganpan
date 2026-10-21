package com.signkorean.domain.usecase

import com.signkorean.domain.model.ProgressReview
import com.signkorean.domain.port.input.ObserveReviewCards
import com.signkorean.domain.port.input.ReviewPageRequest
import com.signkorean.domain.port.output.LearningRepository
import kotlinx.coroutines.flow.Flow

class ObserveReviewCardsUseCase(
    private val learningRepository: LearningRepository,
) : ObserveReviewCards {
    override fun invoke(request: ReviewPageRequest): Flow<List<ProgressReview>> =
        learningRepository.observeReviewCards(limit = request.limit, offset = request.offset)
}
