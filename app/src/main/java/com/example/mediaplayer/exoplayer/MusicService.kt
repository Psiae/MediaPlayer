package com.example.mediaplayer.exoplayer

import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.mediaplayer.exoplayer.callbacks.NotificationListener
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

class MusicService: MediaBrowserServiceCompat() {

    companion object {
        val TAG = MusicService::class.java.simpleName
    }

    @Inject // Inject dataSourceFactory
    lateinit var dataSourceFactory: DefaultDataSource.Factory

    @Inject // Inject exoPlayer by ServiceModule
    lateinit var exoPlayer: ExoPlayer

    // service Job & Scope for Coroutines
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // Notification
    private lateinit var musicNotificationManager: NotificationManager

    // MediaSession API
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    var isForegroundService = false

    override fun onCreate() {
        super.onCreate()

        // get ActivityIntent from packageManager as PendingIntent
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

        }

        // set the mediaSessionConnector to mediaSession and set the player to Injected ExoPlayer
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(exoPlayer)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
    ) {
        TODO("Not yet implemented")
    }
}