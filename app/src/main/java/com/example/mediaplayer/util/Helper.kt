package com.example.mediaplayer.util

import android.os.Build

object VersionHelper {
    fun isNougat() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    fun isOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    fun isPie() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    fun isQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    fun isR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    fun isSnowCone() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

var isPaused = false

data class Perms(
    val permission: String,
    val requestId: Int = 130,
    val msg: String? = null
)