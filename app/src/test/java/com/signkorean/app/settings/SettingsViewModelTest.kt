package com.signkorean.app.settings

import com.signkorean.domain.model.AudioPlayRequest
import com.signkorean.domain.model.AudioPlaybackState
import com.signkorean.domain.model.ProgressCategory
import com.signkorean.domain.model.ProgressSnapshot
import com.signkorean.domain.model.ReminderChangedEvent
import com.signkorean.domain.port.input.ConfigureReminder
import com.signkorean.domain.port.input.ConfigureReminderResult
import com.signkorean.domain.port.input.ChangeAppLanguage
import com.signkorean.domain.port.input.ChangeAppLanguageResult
import com.signkorean.domain.port.input.ObserveProgress
import com.signkorean.domain.port.output.AudioPlayerPort
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.testsupport.FakeUserSettingsRepository
import com.signkorean.testsupport.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `learning settings are persisted immediately`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            val viewModel = SettingsViewModel(repository, RecordingAudioPlayer(), RecordingAnalyticsSink())

            viewModel.selectSessionLength(10)
            viewModel.toggleRomanization()
            viewModel.toggleAutoPlay()
            viewModel.toggleReduceMotion()
            advanceUntilIdle()

            val settings = repository.settings.first()
            assertEquals(10, settings.sessionLength)
            assertTrue(settings.showRomanization)
            assertTrue(settings.autoPlayAudio)
            assertTrue(settings.reduceMotion)
            assertFalse(settings.followSystemReduceMotion)
            assertEquals(10, viewModel.uiState.value.sessionLength)
        }

    @Test
    fun `unsupported session lengths are ignored`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            val viewModel = SettingsViewModel(repository, RecordingAudioPlayer(), RecordingAnalyticsSink())

            viewModel.selectSessionLength(15)
            advanceUntilIdle()

            assertEquals(20, repository.settings.first().sessionLength)
        }

    @Test
    fun `selecting an app language persists it and emits a refresh effect`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            val languageUseCase = RecordingChangeAppLanguage(repository)
            val viewModel =
                SettingsViewModel(
                    repository,
                    RecordingAudioPlayer(),
                    RecordingAnalyticsSink(),
                    changeAppLanguage = languageUseCase,
                )
            advanceUntilIdle()

            viewModel.selectLanguage("ja")
            advanceUntilIdle()

            assertEquals("ja", repository.settings.first().appLanguageTag)
            assertEquals("ja", repository.settings.first().learningExplanationLanguageTag)
            assertEquals(SettingsEffect.LanguageChanged("ja"), viewModel.effects.first())
            assertEquals(listOf("ja"), languageUseCase.requestedTags)
            assertEquals("ja", viewModel.uiState.value.appLanguageTag)
        }

    @Test
    fun `reminder is off by default and enabling asks for permission before saving`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            val analytics = RecordingAnalyticsSink()
            val viewModel =
                SettingsViewModel(
                    repository,
                    RecordingAudioPlayer(),
                    analytics,
                    configureReminder = RecordingConfigureReminder(repository),
                    observeProgress = ObserveProgress { flowOf(emptyProgressSnapshot()) },
                )
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.reminderEnabled)
            viewModel.toggleReminder()

            assertEquals(SettingsEffect.ReminderPermissionRequested, viewModel.effects.first())
            assertFalse(repository.settings.first().reminderEnabled)
        }

    @Test
    fun `denied reminder permission keeps setting off and emits guidance effect`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            val analytics = RecordingAnalyticsSink()
            val viewModel =
                SettingsViewModel(
                    repository,
                    RecordingAudioPlayer(),
                    analytics,
                    configureReminder = RecordingConfigureReminder(repository),
                )
            viewModel.onReminderPermissionResult(granted = false)
            advanceUntilIdle()

            assertFalse(repository.settings.first().reminderEnabled)
            assertEquals(SettingsEffect.ReminderPermissionDenied, viewModel.effects.first())
            assertEquals(listOf(ReminderChangedEvent(false)), analytics.reminderChanges)
        }

    @Test
    fun `approved permission enables reminder when due cards exist`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            val analytics = RecordingAnalyticsSink()
            val progress =
                ProgressSnapshot(
                    learnedCount = 1,
                    masteredCount = 0,
                    dueCount = 1,
                    weeklyActivity = emptyList(),
                    categoryProgress = emptyList(),
                    recentReviews = emptyList(),
                )
            val viewModel =
                SettingsViewModel(
                    repository,
                    RecordingAudioPlayer(),
                    analytics,
                    configureReminder = RecordingConfigureReminder(repository),
                    observeProgress = ObserveProgress { flowOf(progress) },
                )
            advanceUntilIdle()
            viewModel.onReminderPermissionResult(granted = true)
            advanceUntilIdle()

            assertTrue(repository.settings.first().reminderEnabled)
            assertEquals(SettingsEffect.ReminderChanged(true), viewModel.effects.first())
            assertEquals(listOf(ReminderChangedEvent(true)), analytics.reminderChanges)
        }

    private class RecordingAudioPlayer : AudioPlayerPort {
        private val state = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
        var stopCount = 0

        override fun play(request: AudioPlayRequest) = Unit

        override fun stop() {
            stopCount += 1
        }

        override fun observePlayback() = state
    }

    private class RecordingAnalyticsSink : AnalyticsSink {
        val reminderChanges = mutableListOf<ReminderChangedEvent>()

        override fun recordAppStarted(event: com.signkorean.domain.model.AppStartupEvent) = Unit

        override fun recordReminderChanged(event: ReminderChangedEvent) {
            reminderChanges += event
        }
    }

    private class RecordingConfigureReminder(
        private val repository: FakeUserSettingsRepository,
    ) : ConfigureReminder {
        override suspend fun invoke(enabled: Boolean, dueCount: Int): ConfigureReminderResult {
            repository.updateSettings { it.copy(reminderEnabled = enabled && dueCount > 0) }
            return if (!enabled) ConfigureReminderResult.Disabled else if (dueCount > 0) ConfigureReminderResult.Scheduled else ConfigureReminderResult.NoDueCards
        }
    }

    private class RecordingChangeAppLanguage(
        private val repository: FakeUserSettingsRepository,
    ) : ChangeAppLanguage {
        override val supportedLanguageTags: Set<String> = setOf("en", "ja")
        val requestedTags = mutableListOf<String>()

        override suspend fun invoke(languageTag: String): ChangeAppLanguageResult {
            requestedTags += languageTag
            repository.updateSettings {
                it.copy(appLanguageTag = languageTag, learningExplanationLanguageTag = languageTag)
            }
            return ChangeAppLanguageResult.Changed(languageTag)
        }
    }

    private fun emptyProgressSnapshot() =
        ProgressSnapshot(
            learnedCount = 0,
            masteredCount = 0,
            dueCount = 0,
            weeklyActivity = emptyList(),
            categoryProgress = emptyList<ProgressCategory>(),
            recentReviews = emptyList(),
        )
}
