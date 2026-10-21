package com.signkorean.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.model.AudioVoice
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.model.VoiceChangedEvent
import com.signkorean.domain.port.output.AudioPlayerPort
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.feature.settings.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val userSettingsRepository: UserSettingsRepository,
        private val audioPlayer: AudioPlayerPort,
        private val analyticsSink: AnalyticsSink,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(SettingsUiState())
        private val effectChannel = Channel<SettingsEffect>(Channel.BUFFERED)
        private val settingsMutex = Mutex()
        private var selectionJob: Job? = null
        private var sessionLengthJob: Job? = null

        init {
            viewModelScope.launch {
                userSettingsRepository.settings.collect { settings ->
                    mutableUiState.value =
                        SettingsUiState(
                            selectedVoice = settings.audioVoice,
                            sessionLength = settings.sessionLength,
                            showRomanization = settings.showRomanization,
                            autoPlayAudio = settings.autoPlayAudio,
                            reduceMotion = settings.reduceMotion,
                            followSystemReduceMotion = settings.followSystemReduceMotion,
                        )
                }
            }
        }

        val uiState = mutableUiState.asStateFlow()
        val effects = effectChannel.receiveAsFlow()

        fun selectVoice(voice: AudioVoice) {
            if (selectionJob?.isActive == true) return
            // A voice change must never leave the previous provider playing while persistence runs.
            audioPlayer.stop()
            val previousVoice = mutableUiState.value.selectedVoice
            selectionJob = viewModelScope.launch {
                try {
                    settingsMutex.withLock {
                        userSettingsRepository.updateSettings { settings -> settings.copy(audioVoice = voice) }
                    }
                    try {
                        analyticsSink.recordVoiceChanged(VoiceChangedEvent(previousVoice, voice))
                    } catch (_: Exception) {
                        // Analytics must not make a successful setting change look like a failure.
                    }
                    effectChannel.trySend(SettingsEffect.VoiceChanged(voice))
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    effectChannel.trySend(SettingsEffect.VoiceChangeFailed)
                }
            }
        }

        fun selectSessionLength(length: Int) {
            if (length !in UserSettings.SESSION_LENGTH_OPTIONS || sessionLengthJob?.isActive == true) return
            sessionLengthJob =
                viewModelScope.launch {
                    try {
                        settingsMutex.withLock {
                            userSettingsRepository.updateSettings { settings -> settings.copy(sessionLength = length) }
                        }
                        effectChannel.trySend(SettingsEffect.SessionLengthChanged(length))
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        effectChannel.trySend(SettingsEffect.SettingsChangeFailed)
                    }
                }
        }

        fun toggleRomanization() {
            persistSettings { settings -> settings.copy(showRomanization = !settings.showRomanization) }
        }

        fun toggleAutoPlay() {
            persistSettings { settings -> settings.copy(autoPlayAudio = !settings.autoPlayAudio) }
        }

        fun toggleReduceMotion() {
            persistSettings { settings ->
                settings.copy(
                    reduceMotion = !settings.reduceMotion,
                    // A second tap returns to following the system setting. The repository still
                    // applies the system preference with OR semantics when it is enabled.
                    followSystemReduceMotion = !settings.followSystemReduceMotion,
                )
            }
        }

        private fun persistSettings(transform: (UserSettings) -> UserSettings) {
            viewModelScope.launch {
                try {
                    settingsMutex.withLock {
                        userSettingsRepository.updateSettings(transform)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    effectChannel.trySend(SettingsEffect.SettingsChangeFailed)
                }
            }
        }
    }

sealed interface SettingsEffect {
    data class VoiceChanged(
        val voice: AudioVoice,
    ) : SettingsEffect

    data object VoiceChangeFailed : SettingsEffect

    data class SessionLengthChanged(
        val length: Int,
    ) : SettingsEffect

    data object SettingsChangeFailed : SettingsEffect
}
