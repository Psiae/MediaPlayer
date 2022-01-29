package com.example.mediaplayer

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import dagger.hilt.android.HiltAndroidApp
import leakcanary.LeakCanary
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MPApplication : Application() {

    @Inject
    lateinit var player: ExoPlayer

    @SuppressLint("LogNotTimber")
    override fun onCreate() {
        // Plant Timber if the BuildConfig is DEBUG
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else Log.w("Application", "Timber not Planted, ${BuildConfig.DEBUG}")
        leakCanaryConfig(false)
    }

    private fun leakCanaryConfig(isEnable: Boolean = false) {
        LeakCanary.config = LeakCanary.config.copy(dumpHeap = isEnable)
        LeakCanary.showLeakDisplayActivityLauncherIcon(isEnable)
    }
}