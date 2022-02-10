package com.example.mediaplayer.exoplayer

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mediaplayer.R
import com.example.mediaplayer.util.Constants.ACTION_REPEAT
import com.example.mediaplayer.util.Constants.ACTION_REPEAT_SONG_ALL
import com.example.mediaplayer.util.Constants.ACTION_REPEAT_SONG_OFF
import com.example.mediaplayer.util.Constants.ACTION_REPEAT_SONG_ONCE
import com.example.mediaplayer.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.mediaplayer.util.Constants.NOTIFICATION_ID
import com.example.mediaplayer.util.Constants.NOTIFICATION_INTENT_ACTION_REQUEST_CODE
import com.example.mediaplayer.util.Constants.REPEAT_SONG_ALL
import com.example.mediaplayer.util.Constants.REPEAT_SONG_OFF
import com.example.mediaplayer.util.Constants.REPEAT_SONG_ONCE
import com.example.mediaplayer.util.ext.toast
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import timber.log.Timber

class MusicNotificationManager(
    private val context: Context,
    private val sessionToken: MediaSessionCompat.Token,
    private val notificationListener: PlayerNotificationManager.NotificationListener,
    private val player: Player,
    private val newSongCallback: () -> Unit
) {


    private val notificationManager: PlayerNotificationManager

    private fun buildActionIntent(action: String): PendingIntent {
        val intent = Intent().apply {
            this.action = action
            component = ComponentName(context, MusicService::class.java)
        }
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getService(context,
            NOTIFICATION_INTENT_ACTION_REQUEST_CODE, intent, flag
        )
    }

    private fun getRepeatIcon(): Int {
        return when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> R.drawable.exo_media_action_repeat_off
            Player.REPEAT_MODE_ONE -> R.drawable.exo_media_action_repeat_one
            Player.REPEAT_MODE_ALL -> R.drawable.exo_media_action_repeat_all
            else -> R.drawable.ic_baseline_error_outline_24
        }
    }

    fun actionBuilder(action: String): NotificationCompat.Action {
        var icon = R.drawable.ic_baseline_error_outline_24
        when (action) {
            ACTION_REPEAT_SONG_OFF, ACTION_REPEAT_SONG_ONCE, ACTION_REPEAT_SONG_ALL -> icon = getRepeatIcon()
        }
        return NotificationCompat.Action(icon, action,
            PendingIntent.getBroadcast(context, NOTIFICATION_INTENT_ACTION_REQUEST_CODE, Intent(action).setPackage(
                this@MusicNotificationManager.context.packageName), PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
    }

    fun getCustomActions(): MutableMap<String, NotificationCompat.Action> {
        val actionList = mutableMapOf<String, NotificationCompat.Action>()
        when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> actionList.put(
                ACTION_REPEAT_SONG_ONCE,
                actionBuilder(ACTION_REPEAT_SONG_ONCE))
            Player.REPEAT_MODE_ONE -> actionList.put(
                ACTION_REPEAT_SONG_ALL,
                actionBuilder(ACTION_REPEAT_SONG_ALL)
            )
            Player.REPEAT_MODE_ALL, -> actionList.put(
                ACTION_REPEAT_SONG_OFF,
                actionBuilder(ACTION_REPEAT_SONG_OFF)
            )
        }
        return actionList
    }

    val notifActionRepeatOff = NotificationCompat.Action(
        R.drawable.exo_media_action_repeat_off,
        "OFF",
        PendingIntent
            .getBroadcast(context, 123, Intent(REPEAT_SONG_OFF)
                .setPackage(context.packageName),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    )

    val notifActionRepeatOnce = NotificationCompat.Action(R.drawable.exo_media_action_repeat_one, "ONE",
        PendingIntent.getBroadcast(context, 456, Intent(REPEAT_SONG_ONCE).setPackage(context.packageName), PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    )

    val notifActionRepeatAll = NotificationCompat.Action(R.drawable.exo_media_action_repeat_all, "ALL",
        PendingIntent.getBroadcast(context, 311, Intent(REPEAT_SONG_ALL).setPackage(context.packageName),PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    )

    private val myReceiver = object : PlayerNotificationManager.CustomActionReceiver {
        override fun createCustomActions(
            context: Context,
            instanceId: Int,
        ): MutableMap<String, NotificationCompat.Action> {
            Timber.d("createCustomActions")
            return mutableMapOf(
                Pair(REPEAT_SONG_OFF, notifActionRepeatOff),
                Pair(REPEAT_SONG_ONCE, notifActionRepeatOnce),
                Pair(REPEAT_SONG_ALL, notifActionRepeatAll),
                Pair(ACTION_REPEAT, NotificationCompat.Action(getRepeatIcon(), "REPEAT",
                    PendingIntent.getBroadcast(context, 711, Intent(ACTION_REPEAT)
                        .setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                ))
            )
        }

        override fun getCustomActions(player: Player): MutableList<String> {
            Timber.d("getCustomActions $player")
            val actions = mutableListOf<String>()
             when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> {
                    actions.add(REPEAT_SONG_OFF)
                    myCustomReceiver = REPEAT_SONG_OFF
                }
                Player.REPEAT_MODE_ONE -> {
                    actions.add(REPEAT_SONG_ONCE)
                    myCustomReceiver = REPEAT_SONG_ONCE
                }
                Player.REPEAT_MODE_ALL -> {
                    actions.add(REPEAT_SONG_ALL)
                    myCustomReceiver = REPEAT_SONG_ALL
                }
                else -> mutableListOf("ACTION_REPEAT")
            }
            Timber.d("returnCustomAction: ${actions}")
            return actions
        }

        override fun onCustomAction(player: Player, action: String, intent: Intent) {
            when (action) {
                ACTION_REPEAT -> toast(context, "REPEAT_SONG toggled")
                REPEAT_SONG_ALL -> {
                    player.repeatMode = Player.REPEAT_MODE_OFF
                }
                REPEAT_SONG_OFF -> {
                    player.repeatMode = Player.REPEAT_MODE_ONE
                }
                REPEAT_SONG_ONCE -> {
                    player.repeatMode = Player.REPEAT_MODE_ALL
                }
            }
        }
    }

    val fromExo = object : PlayerNotificationManager.CustomActionReceiver {
        override fun getCustomActions(player: Player): MutableList<String> {
            return mutableListOf("TEST")
        }

        override fun createCustomActions(
            context: Context,
            instanceId: Int,
        ): MutableMap<String, NotificationCompat.Action> {
            return mutableMapOf(Pair("TEST",
                NotificationCompat.Action(R.drawable.exo_icon_stop, "Stop",
                    PendingIntent.getBroadcast(context, 123, Intent("TEST").setPackage(context?.packageName), PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )
            ))
        }

        override fun onCustomAction(player: Player, action: String, intent: Intent) {
            when (action) {
                "TEST" -> player.stop()
            }
        }
    }

    var myCustomReceiver = ""

    class MyNotificationManager(
        context: Context,
        channelId: String, notificationId: Int,
        mediaDescriptionAdapter: MediaDescriptionAdapter,
        notificationListener: NotificationListener?,
        customActionReceiver: CustomActionReceiver?,
        smallIconResourceId: Int, playActionIconResourceId: Int,
        pauseActionIconResourceId: Int, stopActionIconResourceId: Int,
        rewindActionIconResourceId: Int,
        fastForwardActionIconResourceId: Int,
        previousActionIconResourceId: Int, nextActionIconResourceId: Int,
        groupKey: String?
    ) : PlayerNotificationManager(context, channelId, notificationId, mediaDescriptionAdapter,
        notificationListener, customActionReceiver, smallIconResourceId, playActionIconResourceId,
        pauseActionIconResourceId, stopActionIconResourceId, rewindActionIconResourceId,
        fastForwardActionIconResourceId, previousActionIconResourceId, nextActionIconResourceId,
        groupKey) {
        override fun createNotification(
            player: Player,
            builder: NotificationCompat.Builder?,
            ongoing: Boolean,
            largeIcon: Bitmap?,
        ): NotificationCompat.Builder? {
            return super.createNotification(player, builder, ongoing, largeIcon)
        }

        /** | prev | << | play/pause | >> | next | custom actions | stop | */

        /** | custom actions | prev | << | play/pause | >> | next | stop | */

        override fun getActions(player: Player): MutableList<String> {
            val actions = super.getActions(player)
            val toReturn = mutableListOf<String>()
            var index = actions.indexOf(REPEAT_SONG_OFF)
            if (index == -1) index = actions.indexOf(REPEAT_SONG_ONCE)
            if (index == -1) index = actions.indexOf(REPEAT_SONG_ALL)
            toReturn.add(actions[index])
            actions.removeAt(index)
            actions.forEach { toReturn.add(it) }
            return toReturn
        }

        override fun getActionIndicesForCompactView(
            actionNames: MutableList<String>,
            player: Player,
        ): IntArray {
            return super.getActionIndicesForCompactView(actionNames, player)
        }

        override fun getOngoing(player: Player): Boolean {
            return super.getOngoing(player)
        }
    }

    init {

        val mediaController = MediaControllerCompat(context, sessionToken)

        notificationManager = MyNotificationManager(context,
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_ID,
            DescriptionAdapter(mediaController),
            notificationListener,
            myReceiver,
            R.drawable.ic_music_note_light,
            R.drawable.ic_play_48_widget_f,
            R.drawable.ic_pause_48_widget_f,
            R.drawable.ic_baseline_close_24,
            R.drawable.ic_baseline_fast_rewind_24,
            R.drawable.ic_baseline_fast_forward_24,
            R.drawable.ic_baseline_skip_previous_24,
            R.drawable.ic_baseline_skip_next_24,
            null
        )

        /*notificationManager = MusicNotificationBuilder(
            context,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID
        ).apply {
            setCustomActionReceiver(myReceiver)
            setSmallIconResourceId(R.drawable.ic_music_note_light)
            setChannelNameResourceId(R.string.media_player)
            setChannelDescriptionResourceId(R.string.currently_playing)
            setNotificationListener(notificationListener)
            setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            setPlayActionIconResourceId(R.drawable.ic_play_48_widget_f)
            setPauseActionIconResourceId(R.drawable.ic_pause_48_widget_f)
            setStopActionIconResourceId(R.drawable.ic_baseline_close_24)
        }.build()*/
        /*notificationManager = PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID
        ).apply {
            setSmallIconResourceId(R.drawable.ic_music_note_light)
            setChannelNameResourceId(R.string.media_player)
            setChannelDescriptionResourceId(R.string.currently_playing)
            setNotificationListener(notificationListener)
            setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            setPlayActionIconResourceId(R.drawable.ic_play_48_widget_f)
            setPauseActionIconResourceId(R.drawable.ic_pause_48_widget_f)
            setStopActionIconResourceId(R.drawable.ic_baseline_close_24)
            setCustomActionReceiver(myReceiver)
        }.build()*/
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
            setUseNextActionInCompactView(true)
            setUsePreviousActionInCompactView(true)
            setPriority(PRIORITY_HIGH)
        }
    }

    private inner class DescriptionAdapter(
        private val mediaController: MediaControllerCompat
    ) : PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player): CharSequence {
            Timber.d("getCurrentContentTitle + SongDurationCallback")
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





















