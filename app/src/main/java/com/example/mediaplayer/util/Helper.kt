package com.example.mediaplayer.util

import android.os.Build

object VersionHelper {
    fun isOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    fun isPie() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    fun isQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}