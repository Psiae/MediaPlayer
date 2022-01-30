package com.example.mediaplayer.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat.*
import androidx.core.graphics.createBitmap
import androidx.media.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mediaplayer.R
import com.example.mediaplayer.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.mediaplayer.util.Constants.NOTIFICATION_ID
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager

class MusicNotificationManager(
    private val context: Context,
    private val sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener,
    private val newSongCallback: () -> Unit
) {

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
            setStopActionIconResourceId(R.drawable.ic_baseline_close_24)
        }.build()
    }

    fun showNotification(player: Player) {
        with(notificationManager) {
            setPlayer(player)
            setMediaSessionToken(sessionToken)
            setUseStopAction(true)
            setUsePlayPauseActions(true)
            setUseNextAction(true)
            setUsePreviousAction(true)
            setUseFastForwardAction(false)
            setUseRewindAction(false)
            setUseChronometer(true)
            setPriority(PRIORITY_HIGH)
        }
    }

    private inner class DescriptionAdapter(
        private val mediaController: MediaControllerCompat
    ) : PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player): CharSequence {
            newSongCallback()
            return mediaController.metadata.description.title.toString()
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return mediaController.sessionActivity
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return mediaController.metadata.description.subtitle.toString()
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            Glide.with(context)
                .asBitmap()
                .load(mediaController.metadata.description.iconUri)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        callback.onBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                })
            return null
        }
    }
}





















