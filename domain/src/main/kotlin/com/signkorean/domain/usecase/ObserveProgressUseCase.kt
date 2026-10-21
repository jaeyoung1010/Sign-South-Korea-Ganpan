package com.signkorean.domain.usecase

import com.signkorean.domain.model.ProgressCategory
import com.signkorean.domain.model.ProgressReview
import com.signkorean.domain.model.ProgressSnapshot
import com.signkorean.domain.port.input.ObserveProgress
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.LearningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveProgressUseCase(
    private val learningRepository: LearningRepository,
    private val cardCatalog: CardCatalog,
    private val clock: ClockPort,
) : ObserveProgress {
    override fun invoke(): Flow<ProgressSnapshot> {
        val now = clock.now()
        val zoneId = clock.zoneId()
        val cards = cardCatalog.getCards()
        return combine(
            learningRepository.observeProgress(now),
            learningRepository.observeRatingActivity(),
            learningRepository.observeReviewCards(limit = RECENT_REVIEW_LIMIT, offset = 0),
        ) { progress, activity, recentReviews ->
            val uniqueProgress = progress.distinctBy { it.cardId }
            val learnedIds = uniqueProgress.mapTo(mutableSetOf()) { it.cardId }
            ProgressSnapshot(
                learnedCount = learnedIds.size,
                masteredCount = uniqueProgress.count { it.mastered },
                dueCount = uniqueProgress.count { it.nextReviewAt != null && !it.nextReviewAt.isAfter(now) },
                weeklyActivity = ProgressStatistics.lastSevenDays(activity.map { it.ratedAt }, now, zoneId),
                categoryProgress = categories(cards, learnedIds),
                recentReviews = recentReviews.distinctBy(ProgressReview::cardId).take(RECENT_REVIEW_LIMIT),
            )
        }
    }

    private fun categories(
        cards: List<com.signkorean.domain.model.SignCard>,
        learnedIds: Set<String>,
    ): List<ProgressCategory> =
        cards
            .groupBy { it.category }
            .map { (key, categoryCards) ->
                val learnedCount = categoryCards.count { it.id in learnedIds }
                ProgressCategory(
                    key = key,
                    learnedCount = learnedCount,
                    totalCount = categoryCards.size,
                    ratio = learnedCount.toFloat() / categoryCards.size,
                )
            }

    private companion object {
        const val RECENT_REVIEW_LIMIT = 3
    }
}
