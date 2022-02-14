package com.example.mediaplayer.util

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.net.toUri
import com.example.mediaplayer.model.data.entities.Song

fun Song.toMediaMetadataCompat(): MediaMetadataCompat {
    val song = this
    return MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.mediaId.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, song.title)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, song.imageUri)
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, song.mediaUri)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, song.imageUri)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, song.artist)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, song.displayName)
        .build()
}

fun Song.toMediaDesc(): MediaDescriptionCompat {
    return MediaDescriptionCompat.Builder()
        .setTitle(this.title)
        .setSubtitle(this.artist)
        .setIconUri(this.imageUri.toUri())
        .setMediaId(this.mediaId.toString())
        .setMediaUri(this.mediaUri.toUri())
        .build()
}
