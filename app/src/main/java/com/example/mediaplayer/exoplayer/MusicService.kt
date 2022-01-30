package com.example.mediaplayer.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.mediaplayer.exoplayer.callbacks.NotificationListener
import com.example.mediaplayer.exoplayer.callbacks.PlaybackPreparer
import com.example.mediaplayer.exoplayer.callbacks.PlayerEventListener
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.Constants.MEDIA_ROOT_ID
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSource
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

class MusicService: MediaBrowserServiceCompat() {

    companion object {
        val TAG: String = MusicService::class.java.simpleName
        var curSongDuration = 0L
            private set
    }

    @Inject // Inject dataSourceFactory
    lateinit var dataSourceFactory: DefaultDataSource.Factory

    @Inject // Inject exoPlayer by ServiceModule
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var musicSource: MusicSource

    var isForegroundService = false

    // service Job & Scope for Coroutines
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // Notification
    private lateinit var musicNotificationManager: NotificationManager

    // MediaSession API
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    // ExoPlayer
    private lateinit var musicPlayerEventListener: PlayerEventListener
    private var curPlayingSong: MediaMetadataCompat? = null
    private var isPlayerInit = false

    override fun onCreate() {
        super.onCreate()

        Timber.d("MusicService onCreate")

        // get ActivityIntent from packageManager as PendingIntent
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val playbackPreparer = PlaybackPreparer(musicSource) {
            curPlayingSong = it
            preparePlayer(
                musicSource.songMeta,
                it,
                true
            )
        }

        // assign MediaSession to MediaSessionCompat tied with activityIntent
        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        // set the MediaBrowserService sessionToken to mediaSession
        sessionToken = mediaSession.sessionToken

        musicNotificationManager = NotificationManager(
            this,
            mediaSession.sessionToken,
            NotificationListener(this),
        ) {
            curSongDuration = exoPlayer.duration
        }

        // set the mediaSessionConnector to mediaSession and set the player to Injected ExoPlayer
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(playbackPreparer)
        mediaSessionConnector.setPlayer(exoPlayer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())

        musicPlayerEventListener = PlayerEventListener(this)
        musicNotificationManager.setNotificationPlayer(player = exoPlayer)

        exoPlayer.addListener(musicPlayerEventListener)
    }

    suspend fun fetchSongMeta(songs: List<Song>) =
        serviceScope.launch { musicSource.fetchSongMeta(songs) }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {

        // called when exoplayer need new MediaDescription
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return musicSource.songMeta[windowIndex].description
        }
    }

    private fun preparePlayer(
        songList: List<MediaMetadataCompat>,
        songToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        val curSongIndex = if (curPlayingSong == null) 0 else songList.indexOf(songToPlay)
        exoPlayer.setMediaSource(musicSource.asConcat(dataSourceFactory))
        exoPlayer.prepare()
        exoPlayer.seekTo(curSongIndex, 0L)
        exoPlayer.playWhenReady = playNow
    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
    ) {
        Timber.d("onLoadChildren")
        when (parentId) {
            MEDIA_ROOT_ID -> {
                val results: Boolean

                with(musicSource) {
                    val list = musicSource.songMeta
                    if (!isPlayerInit && list.isNotEmpty()) {
                        Timber.d("Player Not Initialized onLoadChildren, initializing...")
                        preparePlayer(list, list[0], false )
                        isPlayerInit = true
                    }
                    results = try {
                        result.sendResult(this.asMediaItem().toMutableList())
                        Timber.d("Result Sent")
                        true
                    } catch (e: Exception) {
                        Timber.d("Unable to send result onLoadChildren")
                        false
                    }
                    if (!results) result.detach()
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }
}