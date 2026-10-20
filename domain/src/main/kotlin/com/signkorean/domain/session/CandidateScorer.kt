package com.signkorean.domain.session

import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel

/** Orders candidates using only domain values, so it remains deterministic and Android-free. */
class CandidateScorer {
    fun prioritize(
        candidates: Collection<SessionCandidate>,
        interests: Set<Interest>,
        level: LearningLevel,
        seed: Long,
    ): List<SessionCandidate> {
        val preferredCategories = interests.flatMapTo(linkedSetOf(), ::categoriesFor)

        return candidates.sortedWith(
            compareBy<SessionCandidate> { candidate -> if (candidate.isNew) 0 else 1 }
                .thenBy { candidate -> if (candidate.category in preferredCategories) 0 else 1 }
                .thenBy { candidate -> levelOrder(candidate.order, level) }
                .thenBy { candidate -> seededTieBreaker(candidate.cardId, seed) }
                .thenBy(SessionCandidate::cardId),
        )
    }

    private fun categoriesFor(interest: Interest): Set<String> =
        when (interest) {
            Interest.LANDMARK -> setOf("LANDMARK")
            Interest.NEIGHBORHOOD -> setOf("NEIGHBORHOOD")
            Interest.MARKET -> setOf("MARKET")
            Interest.TRANSPORT -> setOf("TRANSPORT")
            Interest.FOOD -> setOf("MARKET")
            Interest.SHOPPING -> setOf("MARKET", "NEIGHBORHOOD")
        }

    private fun levelOrder(
        order: Int,
        level: LearningLevel,
    ): Long =
        when (level) {
            LearningLevel.BEGINNER,
            LearningLevel.NOT_SURE,
            -> order.toLong()

            LearningLevel.ELEMENTARY -> -order.toLong()
        }

    private fun seededTieBreaker(
        cardId: String,
        seed: Long,
    ): Long {
        var value = seed xor cardId.hashCode().toLong()
        value = value xor (value ushr 33)
        value *= -49064778989728563L
        value = value xor (value ushr 33)
        return value
    }
}
