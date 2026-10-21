package com.signkorean.domain.usecase

import com.signkorean.domain.model.CardRatingUndoneEvent
import com.signkorean.domain.port.input.UndoRating
import com.signkorean.domain.port.input.UndoRatingRequest
import com.signkorean.domain.port.input.UndoRatingResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.LearningRepository
import kotlinx.coroutines.CancellationException

class UndoRatingUseCase(
    private val learningRepository: LearningRepository,
    private val analyticsSink: AnalyticsSink,
) : UndoRating {
    override suspend fun invoke(request: UndoRatingRequest): UndoRatingResult {
        val result =
            try {
                learningRepository.undoLastRating(request)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                UndoRatingResult.Failed
            }
        if (result is UndoRatingResult.Undone) {
            try {
                analyticsSink.recordRatingUndone(
                    CardRatingUndoneEvent(
                        cardId = result.cardId,
                        previousRating = result.rating,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
            }
        }
        return result
    }
}
