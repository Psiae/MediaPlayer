package com.example.mediaplayer.exoplayer.callbacks

import com.example.mediaplayer.exoplayer.MusicService
import com.example.mediaplayer.util.ext.toast
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import timber.log.Timber

// Listener class for Exoplayer
class PlayerEventListener(
    private val musicService: MusicService
): Player.Listener {

    private var whenReady: Boolean? = null

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_IDLE -> { Timber.d("Player State Idle") }
            Player.STATE_BUFFERING -> { Timber.d("Player State Buffering") }
            Player.STATE_READY -> {
                if (!(whenReady ?: return)) musicService.stopForeground(false)
            }
            Player.STATE_ENDED -> { Timber.d( "Player State Ended")}
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
        this.whenReady = playWhenReady
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        toast(musicService, "Playback Error Occurred", short = true, blockable = false)
    }
}