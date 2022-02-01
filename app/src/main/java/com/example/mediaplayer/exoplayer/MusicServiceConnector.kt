package com.example.mediaplayer.exoplayer

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mediaplayer.util.Constants.NETWORK_ERROR
import com.example.mediaplayer.util.Event
import com.example.mediaplayer.util.Resource
import com.example.mediaplayer.util.ext.toast
import kotlinx.coroutines.*
import timber.log.Timber

class MusicServiceConnector(
    private val context: Context
) {
    private val _isConnected = MutableLiveData<Event<Resource<Boolean>>>()
    val isConnected: LiveData<Event<Resource<Boolean>>> = _isConnected

    private val _networkError = MutableLiveData<Event<Resource<Boolean>>>()
    val networkError: LiveData<Event<Resource<Boolean>>> = _networkError

    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()
    val playbackState: LiveData<PlaybackStateCompat?> = _playbackState

    private val _curPlayingSong = MutableLiveData<MediaMetadataCompat?>()
    val curPlayingSong: LiveData<MediaMetadataCompat?>
        get() {
            Timber.d("curPlayingSongConnector LiveData")
            return _curPlayingSong
        }
    
    lateinit var mediaController: MediaControllerCompat

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(
            context,
            MusicService::class.java
        ),
        mediaBrowserConnectionCallback,
        null
    ).apply {
        connect()
    }

    fun checkMediaBrowser(): Boolean {
        return mediaBrowser.isConnected
    }

    fun connectMediaBrowser() {
        if (!mediaBrowser.isConnected) mediaBrowser.connect()
    }

    fun disconnectMediaBrowser() {
        if (mediaBrowser.isConnected) mediaBrowser.disconnect()
    }

    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        Timber.d("MediaBrowser sub")
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        Timber.d("MediaBrowser unsub")
        mediaBrowser.unsubscribe(parentId, callback)
    }

    var commandResent: Boolean = false
    val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    fun sendCommand(command: String, param: Bundle?, callback: (() -> Unit)? ) {
        if (this::mediaController.isInitialized) {
            mediaController.sendCommand(command, param,
                object : ResultReceiver(Handler(Looper.getMainLooper())) {
                    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                        super.onReceiveResult(resultCode, resultData)
                        Timber.d("onReceiveChildrenResult")
                    }
                }
            )
            Timber.d("command Sent")
            commandResent = false
        } else if (!commandResent) {
            toast(context, "Please Wait", false)
            scope.launch {
                delay(500)
                Timber.d("command Resent")
                commandResent = true
                sendCommand(command, param, callback)
            }.invokeOnCompletion { scope.cancel() }
        } else if (!mediaBrowser.isConnected && commandResent) {
            Timber.d("MediaBrowser Connected")
            mediaBrowser.connect()
            sendCommand(command, param, callback)
        } else toast(context, "Exception Occurred Please Restart")
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            Timber.d("ConnectionCallback CONNECTED")
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            _isConnected.postValue(Event(Resource.success(true)))
        }

        override fun onConnectionSuspended() {
            Timber.d("ConnectionCallback SUSPENDED")

            _isConnected.postValue(Event(Resource.error(
                "The connection was suspended", false
            )))
        }

        override fun onConnectionFailed() {
            Timber.d("ConnectionCallback FAILED")

            _isConnected.postValue(Event(Resource.error(
                "Couldn't connect to media browser", false
            )))
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _playbackState.postValue(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            if (metadata!!.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                != curPlayingSong.value?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            ) {
                Timber.d("onMetadataChanged")
                _curPlayingSong.value = metadata
            }
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when(event) {
                NETWORK_ERROR -> _networkError.postValue(
                    Event(
                        Resource.error(
                            "Couldn't connect to the server. Please check your internet connection.",
                            null
                        )
                    )
                )
            }
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}

















