package com.signkorean.feature.learn

data class LearnCardLayer(
    val cardId: String,
    val role: LearnCardLayerRole,
)

enum class LearnCardLayerRole {
    NextNext,
    Next,
    Active,
}

data class CardPreviewVisuals(
    val scale: Float,
    val alpha: Float,
)

fun cardPreviewVisuals(
    role: LearnCardLayerRole,
    dragOffsetDp: Float,
    reduceMotion: Boolean = false,
): CardPreviewVisuals {
    // Keep the depth cue, but do not continuously interpolate scale/alpha while a reduced-motion
    // user drags the active card.
    val progress = if (reduceMotion) 0f else (kotlin.math.abs(dragOffsetDp) / 90f).coerceIn(0f, 1f)
    return when (role) {
        LearnCardLayerRole.Next -> CardPreviewVisuals(scale = 0.96f + 0.04f * progress, alpha = 0.9f + 0.1f * progress)
        LearnCardLayerRole.NextNext -> CardPreviewVisuals(scale = 0.92f + 0.03f * progress, alpha = 0.72f + 0.13f * progress)
        LearnCardLayerRole.Active -> CardPreviewVisuals(scale = 1f, alpha = 1f)
    }
}

/** Returns layers in draw order: back-most card first and active card last. */
fun cardStackLayers(
    activeCardId: String,
    upcomingCardIds: List<String>,
): List<LearnCardLayer> =
    buildList {
        upcomingCardIds
            .take(2)
            .reversed()
            .forEachIndexed { index, cardId ->
                add(
                    LearnCardLayer(
                        cardId = cardId,
                        role = if (index == 0 && upcomingCardIds.size > 1) LearnCardLayerRole.NextNext else LearnCardLayerRole.Next,
                    ),
                )
            }
        add(LearnCardLayer(cardId = activeCardId, role = LearnCardLayerRole.Active))
    }
