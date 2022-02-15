package com.example.mediaplayer.exoplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.mediaplayer.exoplayer.MusicNotificationManager
import com.example.mediaplayer.exoplayer.MusicSource
import com.example.mediaplayer.exoplayer.callbacks.MusicPlaybackPreparer
import com.example.mediaplayer.exoplayer.callbacks.MusicPlayerEventListener
import com.example.mediaplayer.exoplayer.callbacks.MusicPlayerNotificationListener
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.Constants
import com.example.mediaplayer.util.Constants.MEDIA_ROOT_ID
import com.example.mediaplayer.util.Constants.MUSIC_SERVICE
import com.example.mediaplayer.util.Constants.NOTIFICATION_CHANNEL_ID
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

    lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    var isForegroundService = false

    private var curPlayingSong: MediaMetadataCompat? = null

    private var isPlayerInitialized = false

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    companion object {
        var songToPlay: Song? = null
        var seekToPos: Long? = null
        var shouldPlay: Boolean? = null
            set(value) {
                Timber.d("shouldPlay set $value")
                field = value
            }

        var curSongDuration = 0L
            private set
        var curSong: Song? = null
            private set
        var curSongMediaId = 0L
            private set
        var lastSongQueue = -1L
            private set
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            musicSource.mapToSongs(musicSource.musicDatabase.getAllSongs())
            resInitPlayer()
            notifyChildrenChanged(MEDIA_ROOT_ID)
        }

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 444, it,
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
            curSongMediaId = exoPlayer.currentMediaItem?.mediaId?.toLong() ?: -1L
            lastSongQueue = exoPlayer.currentMediaItemIndex.toLong()
        }

        val musicPlaybackPreparer = MusicPlaybackPreparer(musicSource, this, musicServiceConnector) { item, play, ->
            curPlayingSong = item
            preparePlayer(
                musicSource.songs,
                item,
                play,
                "musicPlaybackPreparer",
                0
            )
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession).also {
            it.mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        }
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, "Kylentt",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun startForeground(notification: Notification) {
        ContextCompat.startForegroundService(
            this,
            Intent(applicationContext, this::class.java)
        )
        this.startForeground(Constants.NOTIFICATION_ID, notification)
        isForegroundService = true
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {

        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return try {
                val song = musicSource.songs[windowIndex]
                val extra = Bundle().also { it.putLong("queue", song.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)) }
                MediaDescriptionCompat.Builder()
                    .setTitle(song.description.title)
                    .setSubtitle(song.description.title)
                    .setIconUri(song.description.iconUri)
                    .setMediaUri(song.description.mediaUri)
                    .setMediaId(song.description.mediaId)
                    .setExtras(extra)
                    .build()
            } catch (e: Exception) {
                Timber.d("MusicQueueNavigator Exception")
                musicSource.songs.first().description
            }
        }
    }

    var lastQueue = listOf<Song>()

    fun fetchSongData() = serviceScope.launch { musicSource.fetchMediaData() }

    var retry = true

    fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean,
        caller: String,
        seekTo: Long?
    ) {
        serviceScope.launch {

            try {
                itemToPlay?.let {
                    val curSongIndex = if (curPlayingSong == null) 0
                    else songs.indexOf(itemToPlay)

                    with(exoPlayer) {
                        setMediaSource(musicSource.asMediaSource(dataSourceFactory))
                        prepare()
                        seekTo(curSongIndex, seekTo ?: seekToPos ?: 0)
                        playWhenReady = playNow
                    }
                }

                Timber.d("preparePlayer, ${itemToPlay?.description?.title} $caller")

            } catch (e: Exception) {
                if (e is IllegalStateException) {
                    Timber.e(e)
                    if (retry) preparePlayer(songs, itemToPlay, playNow, caller, seekTo).also {
                        retry = false
                    } else {
                        toast(this@MusicService, "Unable to Prepare ExoPlayer", false, blockable = false)
                    }
                }
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

    private var savedQueue = listOf<Song>()

    fun saveQueue() {
        savedQueue = musicSource.queuedSong
    }

    fun resInitPlayer() {
        isPlayerInitialized = false
    }

    var detached = false
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Timber.d("onLoadChildren ${savedQueue.size}")
        when (parentId) {
            MEDIA_ROOT_ID -> {
                if (musicSource.init && savedQueue.isNotEmpty()) serviceScope.launch {
                    musicSource.mapToSongs(savedQueue)
                }
                result.sendResult(musicSource.asMediaItems())
                if (!isPlayerInitialized && musicSource.songs.isNotEmpty()) {
                    preparePlayer(musicSource.songs, musicSource.songs[0], false, "onLoadChildren", 0)
                    isPlayerInitialized = true
                }
            }
        }
    }
}























