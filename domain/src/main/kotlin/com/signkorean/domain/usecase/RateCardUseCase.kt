package com.signkorean.domain.usecase

import com.signkorean.domain.model.CardRatedEvent
import com.signkorean.domain.port.input.RateCard
import com.signkorean.domain.port.input.RateCardRequest
import com.signkorean.domain.port.input.RateCardResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.LearningRepository

class RateCardUseCase(
    private val learningRepository: LearningRepository,
    private val analyticsSink: AnalyticsSink,
    private val clock: ClockPort,
) : RateCard {
    override suspend fun invoke(request: RateCardRequest): RateCardResult {
        val result =
            runCatching {
                learningRepository.rateCard(request, clock.now())
            }.getOrElse { RateCardResult.Failed }
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
