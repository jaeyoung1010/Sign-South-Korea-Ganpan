package com.signkorean.domain.port.input

import com.signkorean.domain.model.ProgressReview
import kotlinx.coroutines.flow.Flow

fun interface ObserveReviewCards {
    operator fun invoke(request: ReviewPageRequest): Flow<List<ProgressReview>>
}

data class ReviewPageRequest(
    val limit: Int = DEFAULT_PAGE_SIZE,
    val offset: Int = 0,
) {
    init {
        require(limit > 0) { "Review page size must be positive" }
        require(offset >= 0) { "Review page offset must not be negative" }
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
