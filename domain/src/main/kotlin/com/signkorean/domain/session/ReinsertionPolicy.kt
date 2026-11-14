package com.signkorean.domain.session

/** Pure runtime queue policy for spacing a reviewed card within the remaining session. */
object ReinsertionPolicy {
    const val MIN_GAP = 3
    const val MAX_GAP = 5

    fun gapFor(reviewCount: Int): Int = MIN_GAP + ((reviewCount - 1).coerceAtLeast(0) % (MAX_GAP - MIN_GAP + 1))

    fun insertionIndex(
        remainingSize: Int,
        reviewCount: Int,
    ): Int? {
        if (remainingSize < MIN_GAP) return null
        return gapFor(reviewCount).coerceAtMost(remainingSize)
    }

    fun reinsert(
        reviewedCardId: String,
        remainingCardIds: List<String>,
        reviewCount: Int,
    ): List<String> {
        require(reviewedCardId.isNotBlank()) { "Reviewed card ID must not be blank" }
        val insertionIndex = insertionIndex(remainingCardIds.size, reviewCount) ?: return remainingCardIds
        return remainingCardIds.toMutableList().apply { add(insertionIndex, reviewedCardId) }
    }
}
