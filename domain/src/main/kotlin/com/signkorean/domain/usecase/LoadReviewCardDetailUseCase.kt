package com.signkorean.domain.usecase

import com.signkorean.domain.model.ReviewCardDetail
import com.signkorean.domain.port.input.LoadReviewCardDetail
import com.signkorean.domain.port.input.ReviewCardDetailRequest
import com.signkorean.domain.port.output.CardCatalog

class LoadReviewCardDetailUseCase(
    private val cardCatalog: CardCatalog,
) : LoadReviewCardDetail {
    override fun invoke(request: ReviewCardDetailRequest): ReviewCardDetail? {
        val card = cardCatalog.getCard(request.cardId) ?: return null
        val copy = cardCatalog.getLocalizedCopy(request.languageTag).firstOrNull { it.cardId == request.cardId } ?: return null
        return ReviewCardDetail(card = card, copy = copy)
    }
}
