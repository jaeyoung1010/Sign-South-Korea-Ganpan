package com.signkorean.domain.usecase

import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.LocalizedCardCopy
import com.signkorean.domain.model.LocalizedWordPart
import com.signkorean.domain.model.ReviewCardDetail
import com.signkorean.domain.model.SignCard
import com.signkorean.domain.model.VoiceId
import com.signkorean.domain.port.input.ReviewCardDetailRequest
import com.signkorean.domain.port.output.CardCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoadReviewCardDetailUseCaseTest {
    private val card =
        SignCard(
            id = "card-7",
            order = 7,
            korean = "홍대입구",
            category = "NEIGHBORHOOD",
            imageAssetUri = "file:///android_asset/card-7.jpg",
            focus = FocusRect(10f, 10f, 50f, 30f),
            audioAssetUris = mapOf(VoiceId.TYPECAST_EUNKYUNG to "typecast.mp3"),
            contentVersion = 1,
        )
    private val copy =
        LocalizedCardCopy(
            cardId = "card-7",
            languageTag = "en",
            displayMeaning = "Hongdae Entrance",
            pronunciationGuide = "hongdae ipgu",
            wordParts = listOf(LocalizedWordPart("홍대", "Hongdae"), LocalizedWordPart("입구", "entrance")),
            streetNote = "A busy neighborhood landmark.",
            imageAltTextQuestion = "A street sign.",
            imageAltTextAnswer = "A sign reading 홍대입구.",
            searchTerms = listOf("Hongdae"),
            contentVersion = 1,
        )

    @Test
    fun `loads card metadata and localized copy for the requested language`() {
        val result = LoadReviewCardDetailUseCase(FakeCatalog(card, copy))(ReviewCardDetailRequest("card-7", "en"))

        assertEquals(ReviewCardDetail(card, copy), result)
    }

    @Test
    fun `returns no detail when card or localized copy is missing`() {
        val catalog = FakeCatalog(card, copy)

        assertNull(LoadReviewCardDetailUseCase(catalog)(ReviewCardDetailRequest("missing", "en")))
        assertNull(LoadReviewCardDetailUseCase(catalog)(ReviewCardDetailRequest("card-7", "ja")))
    }

    private class FakeCatalog(
        private val card: SignCard,
        private val copy: LocalizedCardCopy,
    ) : CardCatalog {
        override fun getCards(): List<SignCard> = listOf(card)
        override fun getCard(cardId: String): SignCard? = card.takeIf { it.id == cardId }
        override fun getLocalizedCopy(languageTag: String): List<LocalizedCardCopy> = listOf(copy).filter { it.languageTag == languageTag }
        override fun validateBundle() = error("not needed")
    }
}
