package com.example.mediaplayer.exoplayer.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
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

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(
            context,
            MusicService::class.java
        ),
        mediaBrowserConnectionCallback,
        null
    ).apply {}

    private val curplayingIndex = 0

    private val _repeatMode = MutableLiveData<Int>(0)
    val repeatMode: LiveData<Int> get() = _repeatMode

    lateinit var mediaController: MediaControllerCompat

    fun isControllerInit() = this::mediaController.isInitialized

    fun updateRepeatState(state: Int) {
        try {
            _repeatMode.value = state
        } catch (e: Error) {
            _repeatMode.postValue(state)
        }
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
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
                commandResent = true
                delay(500)
                Timber.d("command Resent")
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
            Timber.d("onMetadataChanged ${metadata?.description?.subtitle} ${metadata?.bundle?.get("queue")}")
            _curPlayingSong.postValue(metadata)
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

















