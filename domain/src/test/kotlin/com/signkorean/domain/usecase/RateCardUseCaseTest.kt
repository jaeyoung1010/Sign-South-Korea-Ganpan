package com.signkorean.domain.usecase

import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.CardRatedEvent
import com.signkorean.domain.model.CardReviewState
import com.signkorean.domain.model.RatingInputSource
import com.signkorean.domain.model.SessionCardScore
import com.signkorean.domain.model.SessionCompletion
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.LearningRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class RateCardUseCaseTest {
    @Test
    fun `records analytics only after a successful rating`() =
        runTest {
            val analytics = RecordingAnalytics()
            val repository = RecordingLearningRepository(RateCardResult.Rated(nextPosition = 2, completed = false))

            val result =
                RateCardUseCase(repository, analytics, fixedClock)(
                    RateCardRequest("session-1", 1, "card-2", RatingDecision.GotIt, RatingInputSource.Gesture),
                )

            assertEquals(RateCardResult.Rated(nextPosition = 2, completed = false), result)
            assertEquals(
                listOf(CardRatedEvent("card-2", 2, RatingDecision.GotIt, RatingInputSource.Gesture)),
                analytics.events,
            )
        }

    @Test
    fun `storage failure is retryable and does not emit an event`() =
        runTest {
            val analytics = RecordingAnalytics()
            val repository = RecordingLearningRepository(RateCardResult.Failed)

            val result =
                RateCardUseCase(repository, analytics, fixedClock)(
                    RateCardRequest("session-1", 0, "card-1", RatingDecision.Review, RatingInputSource.Keyboard),
                )

            assertEquals(RateCardResult.Failed, result)
            assertEquals(emptyList<CardRatedEvent>(), analytics.events)
            assertEquals(1, repository.calls)
        }

    @Test
    fun `records completion analytics only when the persisted result includes completion`() =
        runTest {
            val analytics = RecordingAnalytics()
            val completion =
                SessionCompletion(
                    sessionId = "session-1",
                    totalCount = 2,
                    gotItCount = 1,
                    reviewCount = 1,
                    scores = listOf(SessionCardScore("card-1", RatingDecision.GotIt), SessionCardScore("card-2", RatingDecision.Review)),
                    nextReviewAt = null,
                    durationMillis = 7_000,
                )

            RateCardUseCase(
                RecordingLearningRepository(RateCardResult.Rated(nextPosition = 2, completed = true, completion = completion)),
                analytics,
                fixedClock,
            )(RateCardRequest("session-1", 1, "card-2", RatingDecision.Review, RatingInputSource.Gesture))

            assertEquals(1, analytics.completionEvents.size)
            assertEquals(1, analytics.completionEvents.single().gotItCount)
            assertEquals(1, analytics.completionEvents.single().reviewCount)
            assertEquals(7_000, analytics.completionEvents.single().durationMillis)
        }

    @Test
    fun `already rated card does not emit a duplicate event`() =
        runTest {
            val analytics = RecordingAnalytics()
            val result =
                RateCardUseCase(
                    RecordingLearningRepository(RateCardResult.AlreadyRated),
                    analytics,
                    fixedClock,
                )(
                    RateCardRequest("session-1", 0, "card-1", RatingDecision.Review, RatingInputSource.Accessibility),
                )

            assertEquals(RateCardResult.AlreadyRated, result)
            assertEquals(emptyList<CardRatedEvent>(), analytics.events)
        }

    @Test
    fun `cancellation is propagated instead of being converted to a retryable failure`() =
        runTest {
            val analytics = RecordingAnalytics()
            val repository =
                object : LearningRepository {
                    override fun getReviewState(cardId: String): CardReviewState = CardReviewState.NotFound

                    override suspend fun rateCard(
                        request: RateCardRequest,
                        ratedAt: Instant,
                    ): RateCardResult = throw CancellationException("test cancellation")
                }

            var propagated = false
            try {
                RateCardUseCase(repository, analytics, fixedClock)(
                    RateCardRequest("session-1", 0, "card-1", RatingDecision.Review, RatingInputSource.Gesture),
                )
            } catch (_: CancellationException) {
                propagated = true
            }

            assertTrue(propagated)
            assertEquals(emptyList<CardRatedEvent>(), analytics.events)
        }

    private class RecordingLearningRepository(
        private val result: RateCardResult,
    ) : LearningRepository {
        var calls = 0

        override fun getReviewState(cardId: String): CardReviewState = CardReviewState.NotFound

        override suspend fun rateCard(
            request: RateCardRequest,
            ratedAt: Instant,
        ): RateCardResult {
            calls += 1
            return result
        }
    }

    private class RecordingAnalytics : AnalyticsSink {
        val events = mutableListOf<CardRatedEvent>()
        val completionEvents = mutableListOf<com.signkorean.domain.model.SessionCompletedEvent>()

        override fun recordAppStarted(event: AppStartupEvent) = Unit

        override fun recordCardRated(event: CardRatedEvent) {
            events += event
        }

        override fun recordSessionCompleted(event: com.signkorean.domain.model.SessionCompletedEvent) {
            completionEvents += event
        }
    }

    private companion object {
        val fixedClock =
            object : ClockPort {
                override fun now(): Instant = Instant.parse("2026-10-21T00:00:00Z")

                override fun zoneId(): ZoneId = ZoneId.of("UTC")
            }
    }
}
