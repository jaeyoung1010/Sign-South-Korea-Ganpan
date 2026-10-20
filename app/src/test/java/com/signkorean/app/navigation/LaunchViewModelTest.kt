package com.signkorean.app.navigation

import com.signkorean.domain.model.AppInitializationResult
import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.port.input.InitializeApp
import com.signkorean.testsupport.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LaunchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `keeps splash loading until initialization has one final destination`() =
        runTest {
            val result = CompletableDeferred<AppInitializationResult>()
            val viewModel = LaunchViewModel(InitializeApp { result.await() })

            assertEquals(LaunchUiState.Loading, viewModel.uiState.value)
            result.complete(
                AppInitializationResult.Ready(
                    destination = LaunchDestination.HOME,
                    hasResumableSession = true,
                    usedDefaultSettings = false,
                ),
            )
            advanceUntilIdle()

            assertEquals(
                LaunchUiState.Ready(
                    destination = LaunchDestination.HOME,
                    hasResumableSession = true,
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `retry replaces recoverable error with a ready destination`() =
        runTest {
            var attempts = 0
            val viewModel =
                LaunchViewModel(
                    InitializeApp {
                        attempts += 1
                        if (attempts == 1) {
                            AppInitializationResult.RecoverableFailure.Database
                        } else {
                            AppInitializationResult.Ready(
                                destination = LaunchDestination.ONBOARDING,
                                hasResumableSession = false,
                                usedDefaultSettings = false,
                            )
                        }
                    },
                )
            advanceUntilIdle()

            assertEquals(LaunchUiState.Error(LaunchFailure.Database), viewModel.uiState.value)
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(
                LaunchUiState.Ready(LaunchDestination.ONBOARDING, hasResumableSession = false),
                viewModel.uiState.value,
            )
        }
}
