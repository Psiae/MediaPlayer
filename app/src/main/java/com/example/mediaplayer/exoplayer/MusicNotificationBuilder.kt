package com.example.mediaplayer.exoplayer

import android.content.Context
import androidx.annotation.IntRange
import com.google.android.exoplayer2.ui.PlayerNotificationManager

class MusicNotificationBuilder(
    context: Context,
    @IntRange(from = 1) notificationId: Int,
    channelId: String
) : PlayerNotificationManager.Builder(context, notificationId, channelId) {

    override fun setChannelNameResourceId(channelNameResourceId: Int): PlayerNotificationManager.Builder {
        return super.setChannelNameResourceId(channelNameResourceId)
    }

    override fun setChannelDescriptionResourceId(channelDescriptionResourceId: Int): PlayerNotificationManager.Builder {
        return super.setChannelDescriptionResourceId(channelDescriptionResourceId)
    }

    override fun setChannelImportance(channelImportance: Int): PlayerNotificationManager.Builder {
        return super.setChannelImportance(channelImportance)
    }

    override fun setNotificationListener(notificationListener: PlayerNotificationManager.NotificationListener): PlayerNotificationManager.Builder {
        return super.setNotificationListener(notificationListener)
    }

    override fun setCustomActionReceiver(customActionReceiver: PlayerNotificationManager.CustomActionReceiver): PlayerNotificationManager.Builder {
        return super.setCustomActionReceiver(customActionReceiver)
    }

    override fun setSmallIconResourceId(smallIconResourceId: Int): PlayerNotificationManager.Builder {
        return super.setSmallIconResourceId(smallIconResourceId)
    }

    override fun setPlayActionIconResourceId(playActionIconResourceId: Int): PlayerNotificationManager.Builder {
        return super.setPlayActionIconResourceId(playActionIconResourceId)
    }

    override fun setPauseActionIconResourceId(pauseActionIconResourceId: Int): PlayerNotificationManager.Builder {
        return super.setPauseActionIconResourceId(pauseActionIconResourceId)
    }

    override fun setStopActionIconResourceId(stopActionIconResourceId: Int): PlayerNotificationManager.Builder {
        return super.setStopActionIconResourceId(stopActionIconResourceId)
    }

    override fun setRewindActionIconResourceId(rewindActionIconResourceId: Int): PlayerNotificationManager.Builder {
        return super.setRewindActionIconResourceId(rewindActionIconResourceId)
    }

    override fun setFastForwardActionIconResourceId(fastForwardActionIconResourceId: Int): PlayerNotificationManager.Builder {
        return super.setFastForwardActionIconResourceId(fastForwardActionIconResourceId)
    }

    override fun setPreviousActionIconResourceId(previousActionIconResourceId: Int): PlayerNotificationManager.Builder {
        return super.setPreviousActionIconResourceId(previousActionIconResourceId)
    }

    override fun setNextActionIconResourceId(nextActionIconResourceId: Int): PlayerNotificationManager.Builder {
        return super.setNextActionIconResourceId(nextActionIconResourceId)
    }

    override fun setGroup(groupKey: String): PlayerNotificationManager.Builder {
        return super.setGroup(groupKey)
    }

    override fun setMediaDescriptionAdapter(mediaDescriptionAdapter: PlayerNotificationManager.MediaDescriptionAdapter): PlayerNotificationManager.Builder {
        return super.setMediaDescriptionAdapter(mediaDescriptionAdapter)
    }

    override fun build(): PlayerNotificationManager {
        return super.build()
    }
}
