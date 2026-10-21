package com.signkorean.feature.learn

import com.signkorean.domain.gesture.RatingDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class CardKeyboardPolicyTest {
    @Test
    fun `arrow keys rate an answer and escape closes the session`() {
        assertEquals(CardKeyboardAction.Rate(RatingDecision.GotIt), cardKeyboardAction(CardKeyboardKey.ArrowLeft, LearnCardStage.Answer))
        assertEquals(CardKeyboardAction.Rate(RatingDecision.Review), cardKeyboardAction(CardKeyboardKey.ArrowRight, LearnCardStage.Answer))
        assertEquals(CardKeyboardAction.Close, cardKeyboardAction(CardKeyboardKey.Escape, LearnCardStage.Answer))
    }

    @Test
    fun `arrow keys on a question ask for the answer first`() {
        assertEquals(CardKeyboardAction.CheckAnswerFirst, cardKeyboardAction(CardKeyboardKey.ArrowLeft, LearnCardStage.Question))
        assertEquals(CardKeyboardAction.CheckAnswerFirst, cardKeyboardAction(CardKeyboardKey.ArrowRight, LearnCardStage.Question))
    }

    @Test
    fun `unrelated keys are ignored`() {
        assertEquals(CardKeyboardAction.Ignored, cardKeyboardAction(CardKeyboardKey.Other, LearnCardStage.Answer))
    }
}
