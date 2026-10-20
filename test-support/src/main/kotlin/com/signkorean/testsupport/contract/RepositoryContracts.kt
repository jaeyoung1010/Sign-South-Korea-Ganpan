package com.signkorean.testsupport.contract

import com.signkorean.domain.model.CardReviewState
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.output.LearningRepository
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class LearningRepositoryContract {
    protected abstract fun createRepository(): LearningRepository

    @Test
    fun missingCardReturnsNotFoundReviewState() {
        val cardId = "missing-card"

        assertContractEquals(
            ContractScenario("LearningRepository", cardId, "initial"),
            CardReviewState.NotFound,
            createRepository().getReviewState(cardId),
        )
    }
}

abstract class SessionRepositoryContract {
    protected abstract fun createRepository(activeSession: Boolean): SessionRepository

    @Test
    fun activeSessionIsObservableFromEmptyAdapter() =
        runTest {
            assertContractEquals(
                ContractScenario("SessionRepository", cardId = null, state = "active"),
                true,
                createRepository(activeSession = true).hasActiveSession(),
            )
        }

    @Test
    fun missingSessionIsObservableFromEmptyAdapter() =
        runTest {
            assertContractEquals(
                ContractScenario("SessionRepository", cardId = null, state = "empty"),
                false,
                createRepository(activeSession = false).hasActiveSession(),
            )
        }
}

abstract class UserSettingsRepositoryContract {
    protected abstract fun createRepository(): UserSettingsRepository

    @Test
    fun defaultSettingsAreObservableFromEmptyAdapter() =
        runTest {
            assertContractEquals(
                ContractScenario("UserSettingsRepository", cardId = null, state = "defaults"),
                UserSettings(),
                createRepository().settings.first(),
            )
        }

    @Test
    fun writesAreObservableFromAdapter() =
        runTest {
            val repository = createRepository()
            val expected = UserSettings(onboardingCompleted = true, sessionLength = 10)

            assertContractEquals(
                ContractScenario("UserSettingsRepository", cardId = null, state = "updated"),
                expected,
                repository.updateSettings { expected },
            )
            assertContractEquals(
                ContractScenario("UserSettingsRepository", cardId = null, state = "updated"),
                expected,
                repository.settings.first(),
            )
        }

    @Test
    fun concurrentFieldUpdatesRetainBothChanges() =
        runTest {
            val repository = createRepository()

            coroutineScope {
                awaitAll(
                    async { repository.updateSettings { it.copy(showRomanization = true) } },
                    async { repository.updateSettings { it.copy(autoPlayAudio = true) } },
                )
            }

            assertContractEquals(
                ContractScenario("UserSettingsRepository", cardId = null, state = "concurrent-updates"),
                UserSettings(showRomanization = true, autoPlayAudio = true),
                repository.settings.first(),
            )
        }
}

data class ContractScenario(
    val repository: String,
    val cardId: String?,
    val state: String,
)

fun <T> assertContractEquals(
    scenario: ContractScenario,
    expected: T,
    actual: T,
) {
    val cardLabel = scenario.cardId ?: "n/a"
    assertEquals(
        "${scenario.repository} contract failed: cardId=$cardLabel, state=${scenario.state}, expected=$expected, actual=$actual",
        expected,
        actual,
    )
}
