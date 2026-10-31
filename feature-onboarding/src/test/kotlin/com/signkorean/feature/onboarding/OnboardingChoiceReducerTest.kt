package com.signkorean.feature.onboarding

import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.ProfileAvatar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingChoiceReducerTest {
    private val reducer = OnboardingChoiceReducer()

    @Test
    fun `level selection is single choice and required before continue`() {
        val initial = OnboardingChoiceUiState()
        val beginner = reducer.reduce(initial, OnboardingChoiceAction.SelectLevel(LearningLevel.BEGINNER))
        val elementary = reducer.reduce(beginner, OnboardingChoiceAction.SelectLevel(LearningLevel.ELEMENTARY))

        assertFalse(initial.canContinueFromLevel)
        assertTrue(beginner.canContinueFromLevel)
        assertEquals(LearningLevel.ELEMENTARY, elementary.selectedLevel)
    }

    @Test
    fun `skipping level chooses beginner`() {
        assertEquals(
            LearningLevel.BEGINNER,
            reducer.reduce(OnboardingChoiceUiState(), OnboardingChoiceAction.SkipLevel).selectedLevel,
        )
    }

    @Test
    fun `interests allow no selections and toggle multiple values`() {
        val initial = OnboardingChoiceUiState()
        val selected =
            reducer
                .reduce(initial, OnboardingChoiceAction.ToggleInterest(Interest.FOOD))
                .let { reducer.reduce(it, OnboardingChoiceAction.ToggleInterest(Interest.TRANSPORT)) }
        val deselected = reducer.reduce(selected, OnboardingChoiceAction.ToggleInterest(Interest.FOOD))

        assertEquals(emptySet<Interest>(), initial.selectedInterests)
        assertEquals(setOf(Interest.FOOD, Interest.TRANSPORT), selected.selectedInterests)
        assertEquals(setOf(Interest.TRANSPORT), deselected.selectedInterests)
    }

    @Test
    fun `profile needs both an avatar and a name before onboarding can finish`() {
        val initial = OnboardingChoiceUiState()
        val avatar = reducer.reduce(initial, OnboardingChoiceAction.SelectProfileAvatar(ProfileAvatar.CHINESE_MAN))
        val complete = reducer.reduce(avatar, OnboardingChoiceAction.UpdateProfileName("Wei"))

        assertFalse(initial.canCompleteProfile)
        assertFalse(avatar.canCompleteProfile)
        assertTrue(complete.canCompleteProfile)
        assertEquals(ProfileAvatar.CHINESE_MAN, complete.selectedProfileAvatar)
    }
}
