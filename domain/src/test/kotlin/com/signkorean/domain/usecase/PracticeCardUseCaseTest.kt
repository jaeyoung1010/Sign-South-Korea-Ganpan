package com.signkorean.domain.usecase

import com.signkorean.domain.port.input.PracticeCardRequest
import com.signkorean.domain.port.input.StartSession
import com.signkorean.domain.port.input.StartSessionRequest
import com.signkorean.domain.port.input.StartSessionResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeCardUseCaseTest {
    @Test
    fun `practice creates a one-card practice request without changing replacement policy`() =
        runTest {
            var captured: StartSessionRequest? = null
            val expected = StartSessionResult.Started("practice-session")
            val useCase =
                PracticeCardUseCase(
                    StartSession { request ->
                        captured = request
                        expected
                    },
                )

            val result = useCase(PracticeCardRequest("card-7"))

            assertEquals(expected, result)
            assertEquals(StartSessionRequest.practice("card-7"), captured)
        }

    @Test
    fun `practice passes an active-session replacement result to the caller`() =
        runTest {
            val active = com.signkorean.domain.model.ActiveSession(completedCount = 1, totalCount = 5, sessionId = "active")
            val useCase = PracticeCardUseCase(StartSession { StartSessionResult.ReplacementRequired(active) })

            assertEquals(StartSessionResult.ReplacementRequired(active), useCase(PracticeCardRequest("card-7")))
        }
}
