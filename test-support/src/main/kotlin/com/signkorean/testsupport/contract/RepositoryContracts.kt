package com.signkorean.testsupport.contract

import com.signkorean.domain.model.CardReviewState
import com.signkorean.domain.port.output.LearningRepository
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
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
    fun activeSessionIsObservableFromEmptyAdapter() {
        assertContractEquals(
            ContractScenario("SessionRepository", cardId = null, state = "active"),
            true,
            createRepository(activeSession = true).hasActiveSession(),
        )
    }

    @Test
    fun missingSessionIsObservableFromEmptyAdapter() {
        assertContractEquals(
            ContractScenario("SessionRepository", cardId = null, state = "empty"),
            false,
            createRepository(activeSession = false).hasActiveSession(),
        )
    }
}

abstract class UserSettingsRepositoryContract {
    protected abstract fun createRepository(onboardingComplete: Boolean): UserSettingsRepository

    @Test
    fun defaultOnboardingStateIsObservableFromEmptyAdapter() {
        assertContractEquals(
            ContractScenario("UserSettingsRepository", cardId = null, state = "defaults"),
            false,
            createRepository(onboardingComplete = false).isOnboardingComplete(),
        )
    }

    @Test
    fun completedOnboardingStateIsObservableFromEmptyAdapter() {
        assertContractEquals(
            ContractScenario("UserSettingsRepository", cardId = null, state = "completed"),
            true,
            createRepository(onboardingComplete = true).isOnboardingComplete(),
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
