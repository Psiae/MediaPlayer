package com.example.mediaplayer.exoplayer.callbacks

import android.widget.Toast
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.example.mediaplayer.exoplayer.MusicService
import com.google.android.exoplayer2.PlaybackException

class MusicPlayerEventListener(
    private val musicService: MusicService
) : Player.Listener {

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {

    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(musicService, "An unknown error occured", Toast.LENGTH_LONG).show()
    }
}