package com.signkorean.domain.usecase

import com.signkorean.domain.model.LearnSessionAnswer
import com.signkorean.domain.model.LearnSessionError
import com.signkorean.domain.model.LearnSessionPhoto
import com.signkorean.domain.model.LearnSessionPreview
import com.signkorean.domain.model.LoadLearnSessionResult
import com.signkorean.domain.port.input.LoadLearnSession
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.flow.first

class LoadLearnSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val cardCatalog: CardCatalog,
    private val userSettingsRepository: UserSettingsRepository,
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
        val settings = userSettingsRepository.settings.first()
        val localizedCopy =
            cardCatalog
                .getLocalizedCopy(settings.learningExplanationLanguageTag)
                .firstOrNull { it.cardId == card.id }
                ?: return LoadLearnSessionResult.Invalid(LearnSessionError.Unavailable)
        return LoadLearnSessionResult.Active(
            sessionId = session.sessionId,
            card = card,
            position = session.currentIndex + 1,
            totalCount = session.cards.size,
            photo = LearnSessionPhoto.from(card, localizedCopy),
            answer = LearnSessionAnswer.from(localizedCopy, settings.showRomanization),
            answerRevealed = session.cards[session.currentIndex].answerRevealedAt != null,
            upcomingCards =
                session.cards
                    .drop(session.currentIndex + 1)
                    .take(MAX_PREVIEW_COUNT)
                    .mapNotNull { preview ->
                        cardCatalog.getCard(preview.cardId)?.let { card ->
                            LearnSessionPreview(cardId = card.id, imageAssetUri = card.imageAssetUri)
                        }
                    },
        )
    }

    private companion object {
        const val MAX_PREVIEW_COUNT = 2
    }
}
