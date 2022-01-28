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

    const val STORE_PERMISSION_MSG = ""

    const val NOTIFICATION_CHANNEL_ID = "MediaPlayer"
    const val NOTIFICATION_ID = 301
}