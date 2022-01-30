package com.example.mediaplayer.exoplayer.callbacks

import android.content.ComponentName
import android.content.Context
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mediaplayer.exoplayer.MusicService
import timber.log.Timber

class MusicServiceConnector(
    context: Context
) {
    /** This Class is the connector between MainActivity & MusicService */

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean>
        get() {
            Timber.d("isConnected LiveData")
            return _isConnected
        }

    private val _onError = MutableLiveData<Boolean>()
    val onError: LiveData<Boolean>
        get() {
            Timber.d("onError LiveData")
            return _onError
        }

    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()
    val playbackState: LiveData<PlaybackStateCompat?>
        get() {
            Timber.d("playbackState LiveData")
            return _playbackState
        }

    private val _curPlayingSong = MutableLiveData<MediaMetadataCompat?>()
    val curPlayingSong: LiveData<MediaMetadataCompat?>
        get() {
            Timber.d("curPlayingSong LiveData")
            return _curPlayingSong
        }

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context, MusicService::class.java),
        mediaBrowserConnectionCallback,
        null
    ).apply {
        Timber.d("MusicServiceConnector MediaBrowser Connected")
        connect()
    }

    // need session token to be Initialized
    lateinit var mediaController: MediaControllerCompat

    // transportController to communicate with Services
    val transportController
        get() = mediaController.transportControls

    fun subs(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        Timber.d("subs")
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unSubs(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        Timber.d("unSubs")
        mediaBrowser.unsubscribe(parentId, callback)
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            Timber.d("MediaBrowserCallback onConnected")
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            _isConnected.postValue(true)
        }

        override fun onConnectionSuspended() {
            Timber.d("MediaBrowserCallback ConnectionSuspended")
            _isConnected.postValue(false)
        }

        override fun onConnectionFailed() {
            Timber.d("MediaBrowserCallback ConnectionFailed")
        }
    }

    private inner class MediaControllerCallback: MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            Timber.d("MediaController PlaybackState $state")
            _playbackState.postValue(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Timber.d("MediaController MetadataChanged $metadata")
            _curPlayingSong.postValue(metadata)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            Timber.d("SessionEvent: $event")
        }

        override fun onSessionDestroyed() {
            Timber.d("MediaController Session Destroyed")
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}