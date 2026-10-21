package com.signkorean.app.settings

import com.signkorean.domain.model.AudioPlayRequest
import com.signkorean.domain.model.AudioPlaybackState
import com.signkorean.domain.model.AudioVoice
import com.signkorean.domain.model.VoiceChangedEvent
import com.signkorean.domain.port.output.AudioPlayerPort
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.testsupport.FakeUserSettingsRepository
import com.signkorean.testsupport.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    fun `selecting a voice stops playback and persists it`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            val audioPlayer = RecordingAudioPlayer()
            val analytics = RecordingAnalyticsSink()
            val viewModel = SettingsViewModel(repository, audioPlayer, analytics)

            advanceUntilIdle()
            viewModel.selectVoice(AudioVoice.GEMINI_KORE)
            advanceUntilIdle()

            assertEquals(AudioVoice.GEMINI_KORE, repository.settings.first().audioVoice)
            assertEquals(1, audioPlayer.stopCount)
            assertEquals(
                listOf(VoiceChangedEvent(AudioVoice.TYPECAST_EUNKYUNG, AudioVoice.GEMINI_KORE)),
                analytics.voiceChanges,
            )
            assertTrue(viewModel.uiState.value.selectedVoice == AudioVoice.GEMINI_KORE)
        }

    @Test
    fun `settings flow restores the selected voice for a recreated view model`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            repository.updateSettings { it.copy(audioVoice = AudioVoice.GEMINI_KORE) }

            val viewModel = SettingsViewModel(repository, RecordingAudioPlayer(), RecordingAnalyticsSink())
            advanceUntilIdle()

            assertEquals(AudioVoice.GEMINI_KORE, viewModel.uiState.value.selectedVoice)
        }

    @Test
    fun `ignores a second voice tap while the first save is in flight`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            val audioPlayer = RecordingAudioPlayer()
            val viewModel = SettingsViewModel(repository, audioPlayer, RecordingAnalyticsSink())

            viewModel.selectVoice(AudioVoice.GEMINI_KORE)
            viewModel.selectVoice(AudioVoice.TYPECAST_EUNKYUNG)
            advanceUntilIdle()

            assertEquals(AudioVoice.GEMINI_KORE, repository.settings.first().audioVoice)
            assertEquals(1, audioPlayer.stopCount)
        }

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
        val voiceChanges = mutableListOf<VoiceChangedEvent>()

        override fun recordAppStarted(event: com.signkorean.domain.model.AppStartupEvent) = Unit

        override fun recordVoiceChanged(event: VoiceChangedEvent) {
            voiceChanges += event
        }
    }
}
