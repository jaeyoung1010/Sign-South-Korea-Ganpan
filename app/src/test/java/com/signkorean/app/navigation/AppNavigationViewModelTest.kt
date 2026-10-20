package com.signkorean.app.navigation

import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.port.input.StartOnboarding
import com.signkorean.testsupport.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppNavigationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `records onboarding started only for the accepted welcome transition`() =
        runTest {
            var startCount = 0
            val viewModel = AppNavigationViewModel(StartOnboarding { startCount += 1 })

            viewModel.dispatch(AppNavigationAction.LaunchReady(LaunchDestination.ONBOARDING))
            viewModel.dispatch(AppNavigationAction.StartOnboarding)
            viewModel.dispatch(AppNavigationAction.StartOnboarding)
            advanceUntilIdle()

            assertEquals(1, startCount)
            assertEquals(AppRoute.Tutorial, viewModel.uiState.value.currentRoute)
        }
}
