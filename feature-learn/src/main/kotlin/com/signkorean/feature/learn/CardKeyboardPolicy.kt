package com.signkorean.feature.learn

import com.signkorean.domain.gesture.RatingDecision

enum class CardKeyboardKey {
    ArrowLeft,
    ArrowRight,
    Escape,
    Other,
}

sealed interface CardKeyboardAction {
    data class Rate(
        val decision: RatingDecision,
    ) : CardKeyboardAction

    data object CheckAnswerFirst : CardKeyboardAction

    data object Close : CardKeyboardAction

    data object Ignored : CardKeyboardAction
}

fun cardKeyboardAction(
    key: CardKeyboardKey,
    stage: LearnCardStage,
): CardKeyboardAction =
    when (key) {
        CardKeyboardKey.Escape -> {
            CardKeyboardAction.Close
        }

        CardKeyboardKey.Other -> {
            CardKeyboardAction.Ignored
        }

        CardKeyboardKey.ArrowLeft,
        CardKeyboardKey.ArrowRight,
        -> {
            if (stage == LearnCardStage.Question) {
                CardKeyboardAction.CheckAnswerFirst
            } else {
                CardKeyboardAction.Rate(
                    if (key == CardKeyboardKey.ArrowLeft) RatingDecision.GotIt else RatingDecision.Review,
                )
            }
        }
    }
