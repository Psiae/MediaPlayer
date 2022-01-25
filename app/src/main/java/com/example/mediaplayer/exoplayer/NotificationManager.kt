package com.example.mediaplayer.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.bumptech.glide.Glide
import com.example.mediaplayer.R
import com.example.mediaplayer.util.Constants
import com.example.mediaplayer.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.mediaplayer.util.Constants.NOTIFICATION_ID
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.NotificationUtil.IMPORTANCE_HIGH

/*class NotificationManager(
    private val context: Context,
    private val newSongCallback: () -> Unit,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener,
) {
    // Notification Manager from ExoPlayer library
    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)
        notificationManager = PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID
        ).apply {
            setSmallIconResourceId(R.drawable.splash_image_24_transparent)
            setChannelNameResourceId(R.string.media_player)
            setChannelDescriptionResourceId(R.string.currently_playing)
            setNotificationListener(notificationListener)
            setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            this.setChannelImportance(IMPORTANCE_HIGH)
        }.build()
    }

    private inner class DescriptionAdapter(
        private val mediaController: MediaControllerCompat
    ) : PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player): CharSequence {
            return mediaController.metadata.description.title.toString()
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return mediaController.sessionActivity
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return mediaController.metadata.description.subtitle.toString()
        }

        override fun getCurrentSubText(player: Player): CharSequence? {
            return super.getCurrentSubText(player)
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback,
        ): Bitmap? {

        }
    }
}*/
