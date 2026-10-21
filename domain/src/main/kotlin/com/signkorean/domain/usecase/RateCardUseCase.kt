package com.signkorean.domain.usecase

import com.signkorean.domain.model.CardRatedEvent
import com.signkorean.domain.port.input.RateCard
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.LearningRepository
import kotlinx.coroutines.CancellationException

class RateCardUseCase(
    private val learningRepository: LearningRepository,
    private val analyticsSink: AnalyticsSink,
    private val clock: ClockPort,
) : RateCard {
    override suspend fun invoke(request: RateCardRequest): RateCardResult {
        val result =
            try {
                learningRepository.rateCard(request, clock.now())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                RateCardResult.Failed
            }
        if (result is RateCardResult.Rated) {
            runCatching {
                analyticsSink.recordCardRated(
                    CardRatedEvent(
                        cardId = request.cardId,
                        position = request.position + 1,
                        decision = request.decision,
                        source = request.source,
                    ),
                )
            }
        }
        return result
    }
}
