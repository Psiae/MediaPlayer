package com.example.mediaplayer.util

import android.content.Context
import android.os.Build
import com.example.mediaplayer.util.Constants.FOREGROUND_SERVICE
import com.example.mediaplayer.util.Constants.PERMISSION_FOREGROUND_SERVICE_REQUEST_CODE
import com.example.mediaplayer.util.Constants.PERMISSION_WRITE_EXT_REQUEST_CODE
import com.example.mediaplayer.util.Constants.WRITE_STORAGE
import com.vmadalin.easypermissions.EasyPermissions
import timber.log.Timber

object VersionHelper {
    fun isNougat() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N     // Nougat       7.0     24
    fun isNMR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1    // Nougat       7.1     25
    fun isOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O       // Oreo         8.0     26
    fun isOMR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1    // Oreo         8.1     27
    fun isPie() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P        // Pie          9.0     28
    fun isQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q          // Q            10      29
    fun isR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R          // Red Velvet   11      30
    fun isSnowCone() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S   // Snow Cone    12      31
}

object Version {
    fun isNougat() = Build.VERSION.SDK_INT == Build.VERSION_CODES.N     // Nougat       7.0     24
    fun isNMR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1    // Nougat       7.1     25
    fun isOreo() = Build.VERSION.SDK_INT == Build.VERSION_CODES.O       // Oreo         8.0     26
    fun isOMR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1    // Oreo         8.1     27
    fun isPie() = Build.VERSION.SDK_INT == Build.VERSION_CODES.P        // Pie          9.0     28
    fun isQ() = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q          // Q            10      29
    fun isR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.R          // Red Velvet   11      30
    fun isSnowCone() = Build.VERSION.SDK_INT == Build.VERSION_CODES.S   // Snow Cone    12      31
}

object PermsHelper {
    fun checkStoragePermission(context: Context) =
        hasPermission(Perms(WRITE_STORAGE, PERMISSION_WRITE_EXT_REQUEST_CODE), context)
    fun checkForegroundServicePermission(context: Context) =
        if (VersionHelper.isPie()) hasPermission(
            Perms(FOREGROUND_SERVICE, PERMISSION_FOREGROUND_SERVICE_REQUEST_CODE), context
        ) else true

    private fun hasPermission(perms: Perms, context: Context): Boolean {
        return try {
            Timber.d("hasPermission ?: $perms")
            EasyPermissions.hasPermissions(context, perms.permission)
        } catch (e: Exception) {
            Timber.wtf(RuntimeException("$e $perms"))
            false
        }
    }
}

data class Perms(
    val permission: String,
    val requestId: Int = 130,
    val msg: String? = null
)