package com.signkorean.platform.audio

import com.signkorean.domain.model.AudioPlayRequest
import com.signkorean.domain.model.AudioPlaybackFailure
import com.signkorean.domain.model.AudioPlaybackState
import com.signkorean.domain.model.VoiceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Media3AudioPlayerTest {
    @Test
    fun `plays the requested card voice and reports playing`() {
        val factory = RecordingFactory()
        val player = Media3AudioPlayer(factory)

        player.play(request("card-1", VoiceId.TYPECAST_EUNKYUNG, "file:///android_asset/audio/eunkyung/01.mp3"))
        factory.current.emitReady()

        assertEquals("file:///android_asset/audio/eunkyung/01.mp3", factory.current.mediaUri)
        assertEquals(AudioPlaybackState.Playing("card-1", VoiceId.TYPECAST_EUNKYUNG), player.observePlayback().value)

        player.play(request("card-2", VoiceId.GEMINI_KORE, "file:///android_asset/audio/gemini/02.wav"))
        factory.current.emitReady()

        assertEquals("file:///android_asset/audio/gemini/02.wav", factory.current.mediaUri)
        assertEquals(AudioPlaybackState.Playing("card-2", VoiceId.GEMINI_KORE), player.observePlayback().value)
    }

    @Test
    fun `restarting playback stops and releases the previous player`() {
        val factory = RecordingFactory()
        val player = Media3AudioPlayer(factory)
        player.play(request("card-1", VoiceId.TYPECAST_EUNKYUNG, "file:///android_asset/audio/eunkyung/01.mp3"))
        val previous = factory.current

        player.play(request("card-1", VoiceId.TYPECAST_EUNKYUNG, "file:///android_asset/audio/eunkyung/01.mp3"))

        assertTrue(previous.stopCalled)
        assertTrue(previous.releaseCalled)
        assertEquals(2, factory.created.size)
        assertEquals(1, factory.current.prepareCount)
        assertEquals(1, factory.current.playCount)
    }

    @Test
    fun `reports missing and invalid provider assets without creating a player`() {
        val factory = RecordingFactory()
        val player = Media3AudioPlayer(factory)

        player.play(request("card-1", VoiceId.TYPECAST_EUNKYUNG, ""))
        assertEquals(
            AudioPlaybackState.Failed("card-1", VoiceId.TYPECAST_EUNKYUNG, AudioPlaybackFailure.AssetMissing),
            player.observePlayback().value,
        )
        player.play(request("card-1", VoiceId.TYPECAST_EUNKYUNG, "content://provider/audio"))
        assertEquals(
            AudioPlaybackState.Failed("card-1", VoiceId.TYPECAST_EUNKYUNG, AudioPlaybackFailure.InvalidProvider),
            player.observePlayback().value,
        )
        assertTrue(factory.created.isEmpty())
    }

    @Test
    fun `maps decode and audio focus failures and stop returns to idle`() {
        val factory = RecordingFactory()
        val player = Media3AudioPlayer(factory)
        player.play(request("card-1", VoiceId.GEMINI_KORE, "file:///android_asset/audio/gemini/01.wav"))
        factory.current.emitDecodeError()
        assertEquals(
            AudioPlaybackState.Failed("card-1", VoiceId.GEMINI_KORE, AudioPlaybackFailure.Decode),
            player.observePlayback().value,
        )

        player.play(request("card-2", VoiceId.GEMINI_KORE, "file:///android_asset/audio/gemini/02.wav"))
        factory.current.emitAudioFocusDenied()
        assertEquals(
            AudioPlaybackState.Failed("card-2", VoiceId.GEMINI_KORE, AudioPlaybackFailure.AudioFocusDenied),
            player.observePlayback().value,
        )
        player.stop()
        assertEquals(AudioPlaybackState.Idle, player.observePlayback().value)
        assertTrue(factory.current.stopCalled)
        assertTrue(factory.current.releaseCalled)
    }

    private fun request(
        cardId: String,
        voiceId: VoiceId,
        uri: String,
    ) = AudioPlayRequest(cardId, voiceId, uri)
}

private class RecordingFactory : Media3PlayerFactory {
    val created = mutableListOf<RecordingHandle>()
    val current: RecordingHandle
        get() = created.last()

    override fun create(callbacks: Media3PlayerCallbacks): Media3PlayerHandle = RecordingHandle(callbacks).also(created::add)
}

private class RecordingHandle(
    private val callbacks: Media3PlayerCallbacks,
) : Media3PlayerHandle {
    var mediaUri: String? = null
    var prepareCount = 0
    var playCount = 0
    var stopCalled = false
    var releaseCalled = false

    override fun setMediaItem(uri: String) {
        mediaUri = uri
    }

    override fun prepare() {
        prepareCount += 1
    }

    override fun play() {
        playCount += 1
    }

    override fun stop() {
        stopCalled = true
    }

    override fun release() {
        releaseCalled = true
    }

    fun emitReady() = callbacks.onReady()

    fun emitDecodeError() = callbacks.onDecodeError()

    fun emitAudioFocusDenied() = callbacks.onAudioFocusDenied()
}
