package com.signkorean.domain.usecase

import com.signkorean.domain.model.AnswerRevealedEvent
import com.signkorean.domain.port.input.RevealAnswer
import com.signkorean.domain.port.input.RevealAnswerResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.SessionRepository

class RevealAnswerUseCase(
    private val sessionRepository: SessionRepository,
    private val analyticsSink: AnalyticsSink,
    private val clock: ClockPort,
) : RevealAnswer {
    override suspend fun invoke(): RevealAnswerResult {
        val session = sessionRepository.getActiveLearnSession() ?: return RevealAnswerResult.Unavailable
        val currentCard = session.cards.getOrNull(session.currentIndex) ?: return RevealAnswerResult.Unavailable
        val recorded =
            sessionRepository.markAnswerRevealed(
                sessionId = session.sessionId,
                position = currentCard.position,
                cardId = currentCard.cardId,
                revealedAt = clock.now(),
            )
        if (!recorded) return RevealAnswerResult.AlreadyRevealed

        analyticsSink.recordAnswerRevealed(
            AnswerRevealedEvent(cardId = currentCard.cardId, position = currentCard.position + 1),
        )
        return RevealAnswerResult.Revealed
    }
}
