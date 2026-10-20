package com.signkorean.app.home

import com.signkorean.domain.model.ActiveSession
import com.signkorean.domain.model.LearnHomeDashboard
import com.signkorean.domain.model.LearnHomeSession
import com.signkorean.domain.port.input.LoadLearnHome
import com.signkorean.testsupport.MainDispatcherRule
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LearnHomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `maps injected dashboard to resumable home state`() =
        runTest {
            val viewModel =
                LearnHomeViewModel(
                    LoadLearnHome {
                        LearnHomeDashboard(
                            streakDays = 4,
                            session =
                                LearnHomeSession(
                                    length = 10,
                                    estimatedMinutes = 4,
                                    reviewCount = 2,
                                    newCount = 8,
                                    activeSession = ActiveSession(completedCount = 3, totalCount = 10),
                                ),
                            categories = emptyList(),
                            weeklyActivity = emptyList(),
                        )
                    },
                )

            advanceUntilIdle()

            assertEquals(4, viewModel.uiState.value.streakDays)
            assertEquals(10, viewModel.uiState.value.session.length)
            assertEquals(3, viewModel.uiState.value.session.completedCount)
        }

    @Test
    fun `refresh cancels an older dashboard load before publishing newer data`() =
        runTest {
            var calls = 0
            val viewModel =
                LearnHomeViewModel(
                    LoadLearnHome {
                        calls += 1
                        if (calls == 1) {
                            awaitCancellation()
                        }
                        dashboard(streakDays = 9)
                    },
                )
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(2, calls)
            assertEquals(9, viewModel.uiState.value.streakDays)
        }

    private fun dashboard(streakDays: Int) =
        LearnHomeDashboard(
            streakDays = streakDays,
            session = LearnHomeSession(20, 8, 0, 20, activeSession = null),
            categories = emptyList(),
            weeklyActivity = emptyList(),
        )
}
