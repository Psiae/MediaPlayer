package com.example.mediaplayer.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.widget.Toast
import androidx.media.MediaBrowserServiceCompat
import com.example.mediaplayer.exoplayer.callbacks.MusicPlaybackPreparer
import com.example.mediaplayer.exoplayer.callbacks.MusicPlayerEventListener
import com.example.mediaplayer.exoplayer.callbacks.MusicPlayerNotificationListener
import com.example.mediaplayer.util.Constants.MEDIA_ROOT_ID
import com.example.mediaplayer.util.Constants.MUSIC_SERVICE
import com.example.mediaplayer.util.Constants.NETWORK_ERROR
import com.example.mediaplayer.util.ext.toast
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var musicServiceConnector: MusicServiceConnector

    @Inject
    lateinit var dataSourceFactory: DefaultDataSource.Factory

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var musicSource: MusicSource

    private lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    var isForegroundService = false

    private var curPlayingSong: MediaMetadataCompat? = null

    private var isPlayerInitialized = false

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    companion object {
        var curSongDuration = 0L
            private set
        var lastItemIndex = 0
            private set
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            musicSource.fetchMediaData()
        }

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        mediaSession = MediaSessionCompat(this, MUSIC_SERVICE).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this, musicServiceConnector),
            exoPlayer
        ) {
            curSongDuration = exoPlayer.duration
            lastItemIndex = exoPlayer.currentMediaItemIndex
        }

        val musicPlaybackPreparer = MusicPlaybackPreparer(musicSource, this, musicServiceConnector) {
            curPlayingSong = it
            preparePlayer(
                musicSource.songs,
                it,
                true
            )
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            Timber.d("getMediaDescription")
            return try {
                musicSource.songs[windowIndex].description
            } catch (e: Exception) {
                Timber.d("MusicQueueNavigator Exception")
                musicSource.songs.first().description
            }
        }
    }

    fun fetchSongData() = serviceScope.launch { musicSource.fetchMediaData() }

    fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        serviceScope.launch {
            try {
                val curSongIndex = if (curPlayingSong == null) 0 else songs.indexOf(itemToPlay)
                exoPlayer.setMediaSource(musicSource.asMediaSource(dataSourceFactory))
                exoPlayer.prepare()
                exoPlayer.seekTo(curSongIndex, 0L)
                exoPlayer.playWhenReady = playNow
            } catch (e: Exception) {
                Timber.e(e)
                toast(this@MusicService, "Unable to Prepare ExoPlayer", false, blockable = false)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Toast.makeText(this, "DEBUG: SERVICE DESTROYED", Toast.LENGTH_LONG).show()
        serviceScope.cancel()
        Timber.d("Service Destroyed")
        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
        super.onDestroy()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    var sendResult = true

    fun resInitPlayer() {
        isPlayerInitialized = false
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Timber.d("onLoadChildren")
        sendResult = true
        when(parentId) {
            MEDIA_ROOT_ID -> {
                try {
                    val resultsSent = musicSource.whenReady { isInitialized ->
                        try {
                            if (isInitialized) {
                                Timber.d("sendResult")
                                if (!isPlayerInitialized && musicSource.songs.isNotEmpty()) {
                                    preparePlayer(musicSource.songs, musicSource.songs[0], false)
                                    isPlayerInitialized = true
                                }
                                if (sendResult) {
                                    result.sendResult(musicSource.asMediaItems())
                                    sendResult = false
                                }
                            } else {
                                mediaSession.sendSessionEvent(NETWORK_ERROR, null)
                                if (sendResult) result.sendResult(null)
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                            return@whenReady
                        }
                    }
                    if (!resultsSent && sendResult) {
                        result.detach()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Please Wait For Load", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}























