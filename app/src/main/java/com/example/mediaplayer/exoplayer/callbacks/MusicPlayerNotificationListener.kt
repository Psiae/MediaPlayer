package com.example.mediaplayer.exoplayer.callbacks

import android.app.Notification
import android.support.v4.media.MediaBrowserCompat
import com.example.mediaplayer.exoplayer.service.MusicService
import com.example.mediaplayer.exoplayer.service.MusicServiceConnector
import com.example.mediaplayer.util.Constants.MEDIA_ROOT_ID
import com.example.mediaplayer.util.VersionHelper
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import timber.log.Timber
import java.lang.Exception

class MusicPlayerNotificationListener(
    private val musicService: MusicService,
    private val serviceConnector: MusicServiceConnector
) : PlayerNotificationManager.NotificationListener {

    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        super.onNotificationCancelled(notificationId, dismissedByUser)
        musicService.apply {
            stopForeground(true)
            isForegroundService = false
            serviceConnector.unsubscribe(MEDIA_ROOT_ID, object: MediaBrowserCompat.SubscriptionCallback() {})
            stopSelf()
            Timber.d("Notification Cancelled")
        }
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        super.onNotificationPosted(notificationId, notification, ongoing)
        musicService.apply {
            if(ongoing && !isForegroundService) {
                try {
                    startForeground(notification)
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (VersionHelper.isQ()) {
                        createNotificationChannel()
                        startForeground(notification)
                    }
                }
            }
        }
    }
}











