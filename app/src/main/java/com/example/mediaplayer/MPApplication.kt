package com.example.mediaplayer

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MPApplication : Application() {

    @SuppressLint("LogNotTimber")
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else Log.w("Application", "Timber not Planted, ${BuildConfig.DEBUG}")
    }



}