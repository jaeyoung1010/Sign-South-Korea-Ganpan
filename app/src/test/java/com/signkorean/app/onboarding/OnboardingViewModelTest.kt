package com.signkorean.app.onboarding

import androidx.lifecycle.SavedStateHandle
import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.input.CompleteOnboarding
import com.signkorean.testsupport.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `successful completion saves choices before reporting completion`() =
        runTest {
            var saved: Pair<LearningLevel, Set<Interest>>? = null
            val viewModel =
                OnboardingViewModel(
                    SavedStateHandle(),
                    CompleteOnboarding { level, interests ->
                        saved = level to interests
                        UserSettings(onboardingCompleted = true, level = level, interests = interests)
                    },
                )

            viewModel.selectLevel(LearningLevel.NOT_SURE)
            viewModel.toggleInterest(Interest.FOOD)
            viewModel.completeOnboarding()
            advanceUntilIdle()

            assertEquals(LearningLevel.NOT_SURE to setOf(Interest.FOOD), saved)
            assertTrue(viewModel.uiState.value.isCompleted)
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun `save failure keeps user on interests and enables retry`() =
        runTest {
            var attempts = 0
            val viewModel =
                OnboardingViewModel(
                    SavedStateHandle(),
                    CompleteOnboarding { level, interests ->
                        attempts += 1
                        if (attempts == 1) error("storage unavailable")
                        UserSettings(onboardingCompleted = true, level = level, interests = interests)
                    },
                )

            viewModel.skipLevel()
            viewModel.completeOnboarding()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isCompleted)
            assertTrue(viewModel.uiState.value.hasSaveError)
            assertFalse(viewModel.uiState.value.isSaving)

            viewModel.completeOnboarding()
            advanceUntilIdle()

            assertEquals(2, attempts)
            assertTrue(viewModel.uiState.value.isCompleted)
        }
}
