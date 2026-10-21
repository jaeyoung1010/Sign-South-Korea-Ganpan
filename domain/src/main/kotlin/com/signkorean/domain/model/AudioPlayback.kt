package com.signkorean.domain.model

data class AudioPlayRequest(
    val cardId: String,
    val voiceId: VoiceId,
    val assetUri: String,
)

enum class AudioPlaybackFailure {
    AssetMissing,
    InvalidProvider,
    Decode,
    AudioFocusDenied,
    Unknown,
}

sealed interface AudioPlaybackState {
    data object Idle : AudioPlaybackState

    data class Preparing(
        val cardId: String,
        val voiceId: VoiceId,
    ) : AudioPlaybackState

    data class Playing(
        val cardId: String,
        val voiceId: VoiceId,
    ) : AudioPlaybackState

    data class Failed(
        val cardId: String,
        val voiceId: VoiceId,
        val reason: AudioPlaybackFailure,
    ) : AudioPlaybackState
}
