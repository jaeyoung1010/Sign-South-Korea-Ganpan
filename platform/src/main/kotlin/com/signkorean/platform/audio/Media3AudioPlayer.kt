package com.signkorean.platform.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.signkorean.domain.model.AudioPlayRequest
import com.signkorean.domain.model.AudioPlaybackFailure
import com.signkorean.domain.model.AudioPlaybackState
import com.signkorean.domain.port.output.AudioPlayerPort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun interface Media3PlayerFactory {
    fun create(callbacks: Media3PlayerCallbacks): Media3PlayerHandle
}

interface Media3PlayerCallbacks {
    fun onBuffering()

    fun onReady()

    fun onEnded()

    fun onDecodeError()

    fun onAudioFocusDenied()
}

interface Media3PlayerHandle {
    fun setMediaItem(uri: String)

    fun prepare()

    fun play()

    fun stop()

    fun release()
}

/**
 * Media3-backed output adapter. The domain sees only typed playback state and never an Android
 * player or Context. A new request always owns a fresh media item and starts from its beginning.
 */
class Media3AudioPlayer(
    private val factory: Media3PlayerFactory,
) : AudioPlayerPort {
    private val mutableState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    private var player: Media3PlayerHandle? = null
    private var currentRequest: AudioPlayRequest? = null

    override fun play(request: AudioPlayRequest) {
        stop()
        when {
            request.assetUri.isBlank() -> {
                mutableState.value = request.failed(AudioPlaybackFailure.AssetMissing)
                return
            }

            !request.assetUri.startsWith(ANDROID_ASSET_URI_PREFIX) -> {
                mutableState.value = request.failed(AudioPlaybackFailure.InvalidProvider)
                return
            }
        }

        currentRequest = request
        mutableState.value = AudioPlaybackState.Preparing(request.cardId, request.voiceId)
        val callbacks = callbacksFor(request)
        val createdPlayer =
            try {
                factory.create(callbacks)
            } catch (_: Exception) {
                currentRequest = null
                mutableState.value = request.failed(AudioPlaybackFailure.Unknown)
                return
            }
        player = createdPlayer
        try {
            createdPlayer.setMediaItem(request.assetUri)
            createdPlayer.prepare()
            createdPlayer.play()
        } catch (_: Exception) {
            fail(request, AudioPlaybackFailure.Decode)
        }
    }

    override fun stop() {
        val activePlayer = player
        player = null
        currentRequest = null
        activePlayer?.runCatching { stop() }
        activePlayer?.runCatching { release() }
        mutableState.value = AudioPlaybackState.Idle
    }

    override fun observePlayback(): StateFlow<AudioPlaybackState> = mutableState.asStateFlow()

    private fun callbacksFor(request: AudioPlayRequest) =
        object : Media3PlayerCallbacks {
            override fun onBuffering() {
                if (isCurrent(request)) {
                    mutableState.value = AudioPlaybackState.Preparing(request.cardId, request.voiceId)
                }
            }

            override fun onReady() {
                if (isCurrent(request)) {
                    mutableState.value = AudioPlaybackState.Playing(request.cardId, request.voiceId)
                }
            }

            override fun onEnded() {
                if (isCurrent(request)) {
                    stop()
                }
            }

            override fun onDecodeError() {
                if (isCurrent(request)) {
                    fail(request, AudioPlaybackFailure.Decode)
                }
            }

            override fun onAudioFocusDenied() {
                if (isCurrent(request)) {
                    fail(request, AudioPlaybackFailure.AudioFocusDenied)
                }
            }
        }

    private fun fail(
        request: AudioPlayRequest,
        reason: AudioPlaybackFailure,
    ) {
        if (currentRequest !== request) return
        mutableState.value = request.failed(reason)
        val activePlayer = player
        player = null
        currentRequest = null
        activePlayer?.runCatching { stop() }
        activePlayer?.runCatching { release() }
    }

    private fun isCurrent(request: AudioPlayRequest): Boolean = currentRequest === request

    private fun AudioPlayRequest.failed(reason: AudioPlaybackFailure) =
        AudioPlaybackState.Failed(cardId = cardId, voiceId = voiceId, reason = reason)

    private companion object {
        const val ANDROID_ASSET_URI_PREFIX = "file:///android_asset/"
    }
}

/** Production Media3 factory. The Context is kept at the platform boundary only. */
class ExoPlayerAudioPlayerFactory(
    private val context: Context,
) : Media3PlayerFactory {
    override fun create(callbacks: Media3PlayerCallbacks): Media3PlayerHandle =
        ExoPlayerHandle(
            ExoPlayer.Builder(context).build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .build(),
                    true,
                )
                setHandleAudioBecomingNoisy(true)
                addListener(
                    object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_IDLE -> Unit
                                Player.STATE_BUFFERING -> callbacks.onBuffering()
                                Player.STATE_READY -> callbacks.onReady()
                                Player.STATE_ENDED -> callbacks.onEnded()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            callbacks.onDecodeError()
                        }

                        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                            if (playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                                callbacks.onAudioFocusDenied()
                            }
                        }
                    },
                )
            },
        )
}

private class ExoPlayerHandle(
    private val player: ExoPlayer,
) : Media3PlayerHandle {
    override fun setMediaItem(uri: String) {
        player.setMediaItem(MediaItem.fromUri(uri))
    }

    override fun prepare() {
        player.prepare()
    }

    override fun play() {
        player.play()
    }

    override fun stop() {
        player.stop()
    }

    override fun release() {
        player.release()
    }
}
