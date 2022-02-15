package com.example.mediaplayer.exoplayer.callbacks

import android.widget.Toast
import com.example.mediaplayer.exoplayer.service.MusicService
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import timber.log.Timber

class MusicPlayerEventListener(
    private val musicService: MusicService
) : Player.Listener {

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_IDLE -> Timber.d("Player $playbackState IDLE")
            Player.STATE_BUFFERING -> Timber.d("Player $playbackState BUFFERING")
            Player.STATE_READY -> Timber.d("Player $playbackState READY")
            Player.STATE_ENDED -> Timber.d("Player $playbackState ENDED")
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {

    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        with(musicService) {
            this.musicServiceConnector.updateRepeatState(repeatMode)
            when (repeatMode) {
                Player.REPEAT_MODE_OFF -> Unit
//                    toast(this, "Repeat Off", short = true, blockable = false)
                Player.REPEAT_MODE_ONE -> Unit
//                    toast(this, "Repeat This Song", short = true, blockable = false)
                Player.REPEAT_MODE_ALL -> Unit
//                    toast(this, "Repeat All Song", short = true, blockable = false)
                else -> Unit
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        try {
            musicService.exoPlayer.removeMediaItem(musicService.exoPlayer.currentMediaItemIndex)
            Toast.makeText(musicService, "Item Removed", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Toast.makeText(musicService, "An unknown error occurred", Toast.LENGTH_SHORT).show()
    }
}