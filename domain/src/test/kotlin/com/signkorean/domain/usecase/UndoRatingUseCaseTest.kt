package com.signkorean.domain.usecase

import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.CardRatingUndoneEvent
import com.signkorean.domain.model.CardReviewState
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.input.UndoRatingRequest
import com.signkorean.domain.port.input.UndoRatingResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.LearningRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class UndoRatingUseCaseTest {
    @Test
    fun `records rating undone only after the snapshot is restored`() =
        runTest {
            val analytics = RecordingAnalytics()
            val result =
                UndoRatingUseCase(
                    RecordingRepository(
                        UndoRatingResult.Undone("card-1", 0, RatingDecision.Review),
                    ),
                    analytics,
                )(UndoRatingRequest("session-1"))

            assertEquals(UndoRatingResult.Undone("card-1", 0, RatingDecision.Review), result)
            assertEquals(listOf(CardRatingUndoneEvent("card-1", RatingDecision.Review)), analytics.events)
        }

    @Test
    fun `no snapshot does not emit a duplicate undone event`() =
        runTest {
            val analytics = RecordingAnalytics()
            val result =
                UndoRatingUseCase(RecordingRepository(UndoRatingResult.NothingToUndo), analytics)(
                    UndoRatingRequest("session-1"),
                )

            assertEquals(UndoRatingResult.NothingToUndo, result)
            assertEquals(emptyList<CardRatingUndoneEvent>(), analytics.events)
        }

    private class RecordingRepository(
        private val undoResult: UndoRatingResult,
    ) : LearningRepository {
        override fun getReviewState(cardId: String): CardReviewState = CardReviewState.NotFound

        override suspend fun rateCard(
            request: RateCardRequest,
            ratedAt: Instant,
        ): RateCardResult = RateCardResult.Unavailable

        override suspend fun undoLastRating(request: UndoRatingRequest): UndoRatingResult = undoResult
    }

    private class RecordingAnalytics : AnalyticsSink {
        val events = mutableListOf<CardRatingUndoneEvent>()

        override fun recordAppStarted(event: AppStartupEvent) = Unit

        override fun recordRatingUndone(event: CardRatingUndoneEvent) {
            events += event
        }
    }
}
