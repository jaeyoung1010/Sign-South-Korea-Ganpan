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
