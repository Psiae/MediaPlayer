package com.example.mediaplayer

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import leakcanary.LeakCanary
import timber.log.Timber

@HiltAndroidApp
class MPApplication : Application() {

    @SuppressLint("LogNotTimber")
    override fun onCreate() {
        // Plant Timber if the BuildConfig is DEBUG
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else Log.w("Application", "Timber not Planted, ${BuildConfig.DEBUG}")
        leakCanaryConfig(true)
    }

    private fun leakCanaryConfig(isEnable: Boolean = false) {
        LeakCanary.config = LeakCanary.config.copy(dumpHeap = isEnable)
        LeakCanary.showLeakDisplayActivityLauncherIcon(isEnable)
    }
}