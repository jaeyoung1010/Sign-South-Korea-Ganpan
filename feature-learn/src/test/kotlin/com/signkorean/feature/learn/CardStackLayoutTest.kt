package com.signkorean.feature.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

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

    @Test
    fun `next and third card visuals interpolate with horizontal drag`() {
        val nextAtRest = cardPreviewVisuals(LearnCardLayerRole.Next, 0f)
        val nextDragged = cardPreviewVisuals(LearnCardLayerRole.Next, 90f)
        val thirdDragged = cardPreviewVisuals(LearnCardLayerRole.NextNext, 90f)

        assertEquals(0.96f, nextAtRest.scale, 0.001f)
        assertTrue(nextDragged.scale > nextAtRest.scale)
        assertTrue(nextDragged.alpha > nextAtRest.alpha)
        assertTrue(thirdDragged.scale > cardPreviewVisuals(LearnCardLayerRole.NextNext, 0f).scale)
        assertEquals(1f, (nextDragged.alpha * 1000f).roundToInt() / 1000f, 0.001f)
    }

    @Test
    fun `reduced motion keeps preview cards at their resting visual state`() {
        assertEquals(
            cardPreviewVisuals(LearnCardLayerRole.Next, 0f),
            cardPreviewVisuals(LearnCardLayerRole.Next, 90f, reduceMotion = true),
        )
        assertEquals(
            cardPreviewVisuals(LearnCardLayerRole.NextNext, 0f),
            cardPreviewVisuals(LearnCardLayerRole.NextNext, -90f, reduceMotion = true),
        )
    }

    @Test
    fun `reduced motion uses a short fade without exit rotation or translation`() {
        val policy = MotionPolicy(reduceMotion = true)

        assertTrue(policy.ratingAnimationDurationMillis <= 20L)
        assertEquals(0f, policy.cardExitTranslationFactor)
        assertEquals(0f, policy.cardExitRotationDegrees)
    }
}
