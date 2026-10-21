package com.signkorean.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.model.AudioVoice
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
        private val selectionMutex = Mutex()
        private var selectionJob: Job? = null

        init {
            viewModelScope.launch {
                userSettingsRepository.settings.collect { settings ->
                    mutableUiState.value = SettingsUiState(selectedVoice = settings.audioVoice)
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
                    selectionMutex.withLock {
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
    }

sealed interface SettingsEffect {
    data class VoiceChanged(
        val voice: AudioVoice,
    ) : SettingsEffect

    data object VoiceChangeFailed : SettingsEffect
}
