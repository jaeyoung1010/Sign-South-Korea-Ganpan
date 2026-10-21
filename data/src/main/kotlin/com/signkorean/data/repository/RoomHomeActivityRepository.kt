package com.signkorean.data.repository

import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.model.DueCard
import com.signkorean.domain.model.HomeActivitySnapshot
import com.signkorean.domain.port.output.HomeActivityRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant

class RoomHomeActivityRepository(
    private val database: SignKoreanDatabase,
    private val dispatcher: CoroutineDispatcher,
) : HomeActivityRepository {
    override suspend fun loadHomeActivity(now: Instant): HomeActivitySnapshot =
        withContext(dispatcher) {
            val progress = database.cardProgressDao().all()
            HomeActivitySnapshot(
                progressedCardIds = progress.mapTo(mutableSetOf()) { it.cardId },
                dueCardIds =
                    progress
                        .filter {
                            it.nextReviewAt != null &&
                                !it.nextReviewAt.isAfter(
                                    now,
                                )
                        }.mapTo(mutableSetOf()) { it.cardId },
                dueCards =
                    progress
                        .filter { it.nextReviewAt != null && !it.nextReviewAt.isAfter(now) }
                        .map { due ->
                            DueCard(
                                cardId = due.cardId,
                                nextReviewAt = checkNotNull(due.nextReviewAt),
                                reviewCount = due.reviewCount,
                            )
                        },
                reviewedAt = database.sessionCardDao().ratedAt(),
            )
        }
}
