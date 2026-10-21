package com.signkorean.data.repository

import com.signkorean.data.db.CardProgressEntity
import com.signkorean.data.db.RatingActivityRow
import com.signkorean.data.db.RatingCommand
import com.signkorean.data.db.RatingTransactionResult
import com.signkorean.data.db.RoomSessionTransactionStore
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.data.db.UndoTransactionResult
import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.CardProgress
import com.signkorean.domain.model.CardReviewState
import com.signkorean.domain.model.ProgressReview
import com.signkorean.domain.model.RatingActivity
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.input.UndoRatingRequest
import com.signkorean.domain.port.input.UndoRatingResult
import com.signkorean.domain.port.output.LearningRepository
import com.signkorean.domain.review.ReviewPolicy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant

class RoomLearningRepository(
    private val database: SignKoreanDatabase,
    private val transactionStore: RoomSessionTransactionStore,
    private val dispatcher: CoroutineDispatcher,
) : LearningRepository {
    override fun getReviewState(cardId: String): CardReviewState = CardReviewState.NotFound

    override fun observeProgress(now: Instant): Flow<List<CardProgress>> =
        database.cardProgressDao().observeAll().map { progress -> progress.map { it.toDomain() } }

    override fun observeRatingActivity(): Flow<List<RatingActivity>> =
        database.sessionCardDao().observeRatingActivity().map { activity -> activity.mapNotNull { it.toDomain() } }

    override fun observeReviewCards(limit: Int, offset: Int): Flow<List<ProgressReview>> =
        database.sessionCardDao().observeRecentReviews(limit, offset).map { reviews ->
            reviews.map { ProgressReview(cardId = it.cardId, reviewedAt = it.ratedAt) }
        }

    override suspend fun rateCard(
        request: RateCardRequest,
        ratedAt: Instant,
    ): RateCardResult =
        withContext(dispatcher) {
            val previous = database.cardProgressDao().get(request.cardId)?.toDomain()
            val updated = ReviewPolicy.apply(request.cardId, previous, request.decision, ratedAt).toEntity()
            val result =
                transactionStore.tryRate(
                    RatingCommand(
                        sessionId = request.sessionId,
                        position = request.position,
                        cardId = request.cardId,
                        rating = request.decision.toStorageValue(),
                        ratedAt = ratedAt,
                        updatedProgress = updated,
                    ),
                )
            when (result) {
                is RatingTransactionResult.Applied ->
                    RateCardResult.Rated(
                        nextPosition = result.nextPosition,
                        completed = result.completed,
                        completion = result.completion,
                    )
                RatingTransactionResult.AlreadyRated -> RateCardResult.AlreadyRated
                RatingTransactionResult.Unavailable -> RateCardResult.Unavailable
            }
        }

    override suspend fun undoLastRating(request: UndoRatingRequest): UndoRatingResult =
        withContext(dispatcher) {
            when (val result = transactionStore.tryUndoLastRating(request.sessionId)) {
                is UndoTransactionResult.Restored -> {
                    result.rating.toDecision()?.let { rating ->
                        UndoRatingResult.Undone(
                            cardId = result.cardId,
                            position = result.position,
                            rating = rating,
                        )
                    } ?: UndoRatingResult.Unavailable
                }

                UndoTransactionResult.NothingToUndo -> {
                    UndoRatingResult.NothingToUndo
                }

                UndoTransactionResult.Unavailable -> {
                    UndoRatingResult.Unavailable
                }
            }
        }

    private fun RatingDecision.toStorageValue(): String = if (this == RatingDecision.GotIt) "GOT_IT" else "REVIEW"

    private fun CardProgressEntity.toDomain() =
        CardProgress(
            cardId = cardId,
            stage = stage,
            mastered = mastered,
            lastRating = lastRating?.toDecision(),
            lastReviewedAt = lastReviewedAt,
            nextReviewAt = nextReviewAt,
            reviewCount = reviewCount,
            gotItCount = gotItCount,
            version = version,
        )

    private fun RatingActivityRow.toDomain(): RatingActivity? =
        when (rating) {
            "GOT_IT" -> RatingActivity(cardId, RatingDecision.GotIt, ratedAt)
            "REVIEW" -> RatingActivity(cardId, RatingDecision.Review, ratedAt)
            else -> null
        }

    private fun String.toDecision(): RatingDecision? =
        when (this) {
            "GOT_IT" -> RatingDecision.GotIt
            "REVIEW" -> RatingDecision.Review
            else -> null
        }

    private fun CardProgress.toEntity() =
        CardProgressEntity(
            cardId = cardId,
            stage = stage,
            mastered = mastered,
            lastRating = lastRating?.let { if (it == RatingDecision.GotIt) "GOT_IT" else "REVIEW" },
            lastReviewedAt = lastReviewedAt,
            nextReviewAt = nextReviewAt,
            reviewCount = reviewCount,
            gotItCount = gotItCount,
            version = version,
        )
}
