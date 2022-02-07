package com.example.mediaplayer.util

import android.Manifest
import android.annotation.SuppressLint

object Constants {

    // Permissions
    const val PERMISSION_INTERNET_REQUEST_CODE = 131
    const val PERMISSION_FOREGROUND_SERVICE_REQUEST_CODE = 132
    const val PERMISSION_WRITE_EXT_REQUEST_CODE = 133
    const val PERMISSION_READ_EXT_REQUEST_CODE = 134
    const val INTERNET = Manifest.permission.INTERNET
    @SuppressLint("InlinedApi")
    const val FOREGROUND_SERVICE = Manifest.permission.FOREGROUND_SERVICE
    const val READ_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
    const val WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE

    // Control
    const val FULL_SCREEN = "MODE_FULLSCREEN"
    const val PAGER_SCREEN = "MODE_PAGER"
    const val DEFAULT_SCREEN = "MODE_DEFAULT"

    const val FADETHROUGH_IN_DURATION = 600L
    const val  FADETHROUGH_OUT_DURATION = 300L

    // Music Service
    const val MEDIA_ROOT_ID = "Kylentt_root_id"
    const val MUSIC_SERVICE = "MEDIA_PLAYER"

    // Firebase
    const val NETWORK_ERROR = "NETWORK_ERROR"

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "MediaPlayer"
    const val NOTIFICATION_ID = 301
    const val NOTIFICATION_INTENT_ACTION_REQUEST_CODE = 302

    const val ACTION_REPEAT = "ACTION_REPEAT"

    const val ACTION_REPEAT_SONG = "ACTION_REPEAT_SONG"
    const val ACTION_REPEAT_SONG_OFF = "ACTION_REPEAT_SONG_OFF"
    const val ACTION_REPEAT_SONG_ONCE = "ACTION_REPEAT_SONG_ONCE"
    const val ACTION_REPEAT_SONG_ALL = "ACTION_REPEAT)SONG_ALL"

    const val REPEAT_SONG = "REPEAT_SONG"
    const val REPEAT_SONG_OFF = "REPEAT_SONG_OFF"
    const val REPEAT_SONG_ONCE = "REPEAT_SONG_ONCE"
    const val REPEAT_SONG_ALL = "REPEAT_SONG_ALL"

    const val REPEAT_MODE_OFF_INT = 0
    const val REPEAT_MODE_ONE_INT = 1
    const val REPEAT_MODE_ALL_INT = 2

    const val UPDATE_SONG = "UPDATE_SONG"
    const val NOTIFY_CHILDREN = "NOTIFY_CHILDREN"

    const val ARTIST = "ARTIST"
    const val ALBUM = "ALBUM"

    const val UPDATE_INTERVAL = 250L
}