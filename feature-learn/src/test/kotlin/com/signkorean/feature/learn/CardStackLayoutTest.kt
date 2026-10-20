package com.signkorean.feature.learn

import org.junit.Assert.assertEquals
import org.junit.Test

class CardStackLayoutTest {
    @Test
    fun `places next next then next then active card from back to front`() {
        val layers = cardStackLayers(activeCardId = "active", upcomingCardIds = listOf("next", "next-next", "later"))

        assertEquals(
            listOf(
                LearnCardLayer(cardId = "next-next", role = LearnCardLayerRole.NextNext),
                LearnCardLayer(cardId = "next", role = LearnCardLayerRole.Next),
                LearnCardLayer(cardId = "active", role = LearnCardLayerRole.Active),
            ),
            layers,
        )
    }

    @Test
    fun `omits unavailable preview layers instead of drawing empty cards`() {
        assertEquals(
            listOf(LearnCardLayer(cardId = "active", role = LearnCardLayerRole.Active)),
            cardStackLayers(activeCardId = "active", upcomingCardIds = emptyList()),
        )
    }
}
