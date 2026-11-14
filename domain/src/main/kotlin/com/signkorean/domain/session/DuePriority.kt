package com.signkorean.domain.session

import java.time.Instant

/** Orders due work without depending on a clock, database, or Android scheduler. */
object DuePriority {
    fun prioritize(
        candidates: Collection<SessionCandidate>,
        now: Instant,
    ): List<SessionCandidate> =
        candidates.sortedWith(
            compareBy<SessionCandidate> { if (it.isDue) 0 else 1 }
                .thenBy {
                    if (it.isDue && it.nextReviewAt != null && it.nextReviewAt.isBefore(now)) 0 else 1
                }.thenByDescending { if (it.isDue) it.reviewCount else Int.MIN_VALUE }
                .thenBy { it.nextReviewAt ?: Instant.MAX }
                .thenBy(SessionCandidate::order)
                .thenBy(SessionCandidate::cardId),
        )
}
