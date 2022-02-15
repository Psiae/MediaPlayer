package com.example.mediaplayer.exoplayer.callbacks

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.mediaplayer.exoplayer.service.MusicService
import com.example.mediaplayer.exoplayer.service.MusicServiceConnector
import com.example.mediaplayer.exoplayer.MusicSource
import com.example.mediaplayer.util.Constants.MEDIA_ROOT_ID
import com.example.mediaplayer.util.Constants.NOTIFY_CHILDREN
import com.example.mediaplayer.util.Constants.SAVE_QUEUE
import com.example.mediaplayer.util.Constants.UPDATE_QUEUE
import com.example.mediaplayer.util.Constants.UPDATE_SONG
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import timber.log.Timber

class MusicPlaybackPreparer(
    private val musicSource: MusicSource,
    private val musicService: MusicService,
    private val musicServiceConnector: MusicServiceConnector,
    private val playerPrepared: (MediaMetadataCompat?, Boolean) -> Unit,
) : MediaSessionConnector.PlaybackPreparer {

    override fun onCommand(
        player: Player,
        command: String,
        extras: Bundle?,
        cb: ResultReceiver?,
    ): Boolean {
        when (command) {
            UPDATE_QUEUE -> Unit
            UPDATE_SONG -> musicService.fetchSongData()
            NOTIFY_CHILDREN -> musicService.notifyChildrenChanged(MEDIA_ROOT_ID)
            SAVE_QUEUE -> musicService.saveQueue()
        }
        return false
    }

    override fun getSupportedPrepareActions(): Long {
        return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
    }



    override fun onPrepare(playWhenReady: Boolean) = Unit

    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
        Timber.d("PrepareFromMediaId")
        val extra = if (extras?.getLong("queue") != -1L) extras?.getLong("queue") else null
        musicSource.whenReady {
            val itemToPlay = musicSource.songs.find {
                mediaId == it.description.mediaId
                        && (extra == it.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER))
            } ?: return@whenReady
            playerPrepared(itemToPlay, playWhenReady)
        }
    }

    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) = Unit
    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit
}















