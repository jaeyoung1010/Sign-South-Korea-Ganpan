package com.signkorean.domain.usecase

import com.signkorean.domain.model.LearnSessionError
import com.signkorean.domain.model.LoadLearnSessionResult
import com.signkorean.domain.port.input.LoadLearnSession
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.SessionRepository

class LoadLearnSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val cardCatalog: CardCatalog,
) : LoadLearnSession {
    override suspend fun invoke(): LoadLearnSessionResult {
        val session =
            sessionRepository.getActiveLearnSession()
                ?: return LoadLearnSessionResult.Invalid(LearnSessionError.NoActiveSession)

        if (session.cards.isEmpty() || session.requestedLength <= 0) {
            return LoadLearnSessionResult.Invalid(LearnSessionError.EmptySession)
        }
        if (session.cards.size != session.requestedLength) {
            return LoadLearnSessionResult.Invalid(LearnSessionError.InvalidCardCount)
        }
        if (session.cards.map(com.signkorean.domain.model.LearnSessionCard::position) != session.cards.indices.toList()) {
            return LoadLearnSessionResult.Invalid(LearnSessionError.InvalidCardOrder)
        }
        if (session.currentIndex == session.cards.size) return LoadLearnSessionResult.Completed
        if (session.currentIndex !in session.cards.indices) {
            return LoadLearnSessionResult.Invalid(LearnSessionError.InvalidCurrentIndex)
        }

        val cardId = session.cards[session.currentIndex].cardId
        val card = cardCatalog.getCard(cardId) ?: return LoadLearnSessionResult.Invalid(LearnSessionError.UnknownCard)
        return LoadLearnSessionResult.Active(
            sessionId = session.sessionId,
            card = card,
            position = session.currentIndex + 1,
            totalCount = session.cards.size,
        )
    }
}
