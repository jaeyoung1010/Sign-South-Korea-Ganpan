package com.signkorean.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialReducerTest {
    private val reducer = TutorialReducer()

    @Test
    fun `answer is hidden until the user asks to show it`() {
        val initial = TutorialUiState()

        assertFalse(initial.isAnswerVisible)
        assertFalse(initial.canContinue)
        assertEquals(initial, reducer.reduce(initial, TutorialAction.SelectGotIt))
        assertTrue(reducer.reduce(initial, TutorialAction.ShowAnswer).isAnswerVisible)
    }

    @Test
    fun `example rating enables continue without writing a learning record`() {
        val shown = TutorialUiState(isAnswerVisible = true)
        val rated = reducer.reduce(shown, TutorialAction.SelectReview)

        assertEquals(TutorialPracticeRating.Review, rated.practiceRating)
        assertTrue(rated.canContinue)
    }
}
