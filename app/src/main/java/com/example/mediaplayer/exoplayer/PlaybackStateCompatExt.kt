package com.example.mediaplayer.exoplayer

import android.support.v4.media.session.PlaybackStateCompat
import timber.log.Timber

inline val PlaybackStateCompat.isPrepared: Boolean
    get() {
        return when (state) {
            PlaybackStateCompat.STATE_BUFFERING,
            PlaybackStateCompat.STATE_PLAYING,
            PlaybackStateCompat.STATE_PAUSED -> {
                Timber.d("PlaybackStateCompat isPrepared")
                true
            }
            else -> {
                Timber.d("PlaybackStateCompat isNotPrepared")
                false
            }
        }
    }

inline val PlaybackStateCompat.isPlaying: Boolean
    get() {
        return when (state) {
            PlaybackStateCompat.STATE_BUFFERING,
            PlaybackStateCompat.STATE_PLAYING -> {
                Timber.d("PlaybackStateCompat isPlaying")
                true
            }
            else -> {
                Timber.d("PlaybackStateCompat isNotPlaying")
                false
            }
        }
    }

inline val PlaybackStateCompat.isPlayEnabled: Boolean
    get() {
        return when {
            actions and PlaybackStateCompat.ACTION_PLAY != 0L || (actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L && state == PlaybackStateCompat.STATE_PAUSED) -> {
                Timber.d("PlaybackStateCompat Play Enabled")
                true
            }
            else -> {
                Timber.d("PlaybackStateCompat Play Disabled")
                false
            }
        }
    }