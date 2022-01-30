package com.example.mediaplayer.exoplayer

import android.support.v4.media.MediaMetadataCompat
import com.example.mediaplayer.model.data.entities.Song

fun MediaMetadataCompat.toSong(): Song? {
    return description?.let {
        Song(
            mediaId = it.mediaId!!.toLong(),
            title = it.title.toString(),
            mediaUri = it.mediaUri.toString(),
            imageUri = it.iconUri.toString()
        )
    }
}
