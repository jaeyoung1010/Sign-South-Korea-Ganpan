package com.signkorean.domain.port.output

import com.signkorean.domain.model.AudioPlayRequest
import com.signkorean.domain.model.AudioPlaybackState
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayerPort {
    fun play(request: AudioPlayRequest)

    fun stop()

    fun observePlayback(): StateFlow<AudioPlaybackState>
}
