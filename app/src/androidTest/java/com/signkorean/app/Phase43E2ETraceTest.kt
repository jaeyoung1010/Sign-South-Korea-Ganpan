package com.signkorean.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.port.input.ResolveLaunchDestination
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * The fast E2E lane keeps the scenario-to-artifact contract executable. The referenced screen,
 * ViewModel, repository, and platform suites provide the actual flow assertions.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class Phase43E2ETraceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var resolveLaunchDestination: ResolveLaunchDestination

    @Test
    fun quickE2ELaneUsesHiltFakeBindings() {
        hiltRule.inject()

        assertEquals(LaunchDestination.HOME, runBlocking { resolveLaunchDestination() })
    }

    @Test
    fun traceabilityRegistryContainsAllScenarios() {
        assertEquals((1..12).map { "E2E-${it.toString().padStart(2, '0')}" }, Phase43E2ETrace.scenarios.map { it.id })
        assertTrue(Phase43E2ETrace.scenarios.all { it.testReferences.isNotEmpty() })
    }
}

internal data class Phase43E2EScenario(
    val id: String,
    val testReferences: List<String>,
)

internal object Phase43E2ETrace {
    val scenarios =
        listOf(
            Phase43E2EScenario("E2E-01", listOf("OnboardingDestinationsTest", "LearnSessionScreenTest", "ProgressScreenTest")),
            Phase43E2EScenario("E2E-02", listOf("LearnSessionScreenTest")),
            Phase43E2EScenario("E2E-03", listOf("LearnSessionScreenTest", "LearnViewModelTest")),
            Phase43E2EScenario("E2E-04", listOf("LearnViewModelTest", "ProtoUserSettingsRepositoryTest")),
            Phase43E2EScenario("E2E-05", listOf("LearnViewModelTest", "RoomLearningRepositoryTest")),
            Phase43E2EScenario("E2E-06", listOf("Media3AudioPlayerTest", "SettingsViewModelTest")),
            Phase43E2EScenario("E2E-07", listOf("BundleContentAdapterTest", "RoomLearningRepositoryTest")),
            Phase43E2EScenario("E2E-08", listOf("SettingsViewModelTest", "LearnViewModelTest")),
            Phase43E2EScenario("E2E-09", listOf("LearnSessionScreenTest")),
            Phase43E2EScenario("E2E-10", listOf("PointerBoundaryAdapterTest", "LearnSessionScreenTest")),
            Phase43E2EScenario("E2E-11", listOf("SettingsViewModelTest")),
            Phase43E2EScenario("E2E-12", listOf("LearnViewModelTest", "BundleContentAdapterTest")),
        )
}
