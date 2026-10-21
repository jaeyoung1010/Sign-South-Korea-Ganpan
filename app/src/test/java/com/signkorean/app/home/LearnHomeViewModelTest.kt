package com.signkorean.app.home

import com.signkorean.domain.model.ActiveSession
import com.signkorean.domain.model.LearnHomeDashboard
import com.signkorean.domain.model.LearnHomeSession
import com.signkorean.domain.port.input.LoadLearnHome
import com.signkorean.domain.port.input.ResumeSession
import com.signkorean.domain.port.input.ResumeSessionRequest
import com.signkorean.domain.port.input.ResumeSessionResult
import com.signkorean.domain.port.input.StartSession
import com.signkorean.domain.port.input.StartSessionRequest
import com.signkorean.domain.port.input.StartSessionResult
import com.signkorean.testsupport.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
                    noCardsStartSession(),
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
                    noCardsStartSession(),
                )
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(2, calls)
            assertEquals(9, viewModel.uiState.value.streakDays)
        }

    @Test
    fun `persists a successful session before emitting navigation to learn`() =
        runTest {
            val viewModel =
                LearnHomeViewModel(
                    LoadLearnHome { dashboard(streakDays = 0) },
                    StartSession { StartSessionResult.Started("session-1") },
                )
            val effect = async { viewModel.effects.first() }
            runCurrent()

            viewModel.startDailySession()
            advanceUntilIdle()

            assertEquals(HomeSessionEffect.NavigateToLearn, effect.await())
            assertFalse(viewModel.uiState.value.hasStartError)
            assertFalse(viewModel.uiState.value.isStarting)
        }

    @Test
    fun `storage failure keeps home visible and enables retry`() =
        runTest {
            val viewModel =
                LearnHomeViewModel(
                    LoadLearnHome { dashboard(streakDays = 0) },
                    StartSession { error("storage unavailable") },
                )

            viewModel.startDailySession()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.hasStartError)
            assertFalse(viewModel.uiState.value.isStarting)
        }

    @Test
    fun `double start tap invokes the input port once while creation is in flight`() =
        runTest {
            val creation = CompletableDeferred<StartSessionResult>()
            var calls = 0
            val viewModel =
                LearnHomeViewModel(
                    LoadLearnHome { dashboard(streakDays = 0) },
                    StartSession {
                        calls += 1
                        creation.await()
                    },
                )

            viewModel.startDailySession()
            viewModel.startDailySession()
            runCurrent()

            assertEquals(1, calls)
            assertTrue(viewModel.uiState.value.isStarting)

            creation.complete(StartSessionResult.Started("session-1"))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isStarting)
        }

    @Test
    fun `late home refresh retains an in-flight session creation state`() =
        runTest {
            val dashboard = CompletableDeferred<LearnHomeDashboard>()
            val creation = CompletableDeferred<StartSessionResult>()
            val viewModel =
                LearnHomeViewModel(
                    LoadLearnHome { dashboard.await() },
                    StartSession { creation.await() },
                )
            runCurrent()

            viewModel.startDailySession()
            runCurrent()
            dashboard.complete(dashboard(streakDays = 4))
            runCurrent()

            assertTrue(viewModel.uiState.value.isStarting)

            creation.complete(StartSessionResult.Started("session-1"))
            advanceUntilIdle()
        }

    @Test
    fun `category replacement waits for confirmation before retrying with replacement enabled`() =
        runTest {
            val active = ActiveSession(completedCount = 1, totalCount = 5)
            val requests = mutableListOf<StartSessionRequest>()
            val viewModel =
                LearnHomeViewModel(
                    LoadLearnHome { dashboard(streakDays = 0) },
                    StartSession { request ->
                        requests += request
                        if (request.replaceActiveSession) {
                            StartSessionResult.Started("category-session")
                        } else {
                            StartSessionResult.ReplacementRequired(active)
                        }
                    },
                )

            viewModel.startCategorySession("MARKET")
            advanceUntilIdle()

            assertEquals("MARKET", viewModel.uiState.value.pendingCategoryReplacement)
            assertEquals(1, requests.size)

            viewModel.confirmCategoryReplacement()
            advanceUntilIdle()

            assertEquals(listOf(false, true), requests.map { it.replaceActiveSession })
            assertEquals(null, viewModel.uiState.value.pendingCategoryReplacement)
        }

    @Test
    fun `paused session is shown as resumable and resumes before navigation`() =
        runTest {
            val active = ActiveSession(completedCount = 3, totalCount = 10, sessionId = "session-1", isPaused = true)
            var resumedId: String? = null
            val viewModel =
                LearnHomeViewModel(
                    LoadLearnHome { dashboard(streakDays = 0, activeSession = active) },
                    noCardsStartSession(),
                    ResumeSession {
                        resumedId = it.sessionId
                        ResumeSessionResult.Resumed(active.copy(isPaused = false))
                    },
                )
            val effect = async { viewModel.effects.first() }
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.session.isPaused)
            viewModel.startDailySession()
            advanceUntilIdle()

            assertEquals("session-1", resumedId)
            assertEquals(HomeSessionEffect.NavigateToLearn, effect.await())
        }

    private fun noCardsStartSession(): StartSession = StartSession { StartSessionResult.NoCardsAvailable }

    private fun dashboard(
        streakDays: Int,
        activeSession: ActiveSession? = null,
    ) = LearnHomeDashboard(
        streakDays = streakDays,
        session = LearnHomeSession(20, 8, 0, 20, activeSession = activeSession),
        categories = emptyList(),
        weeklyActivity = emptyList(),
    )
}
