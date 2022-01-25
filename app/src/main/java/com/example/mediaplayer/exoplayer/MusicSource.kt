package com.example.mediaplayer.exoplayer

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.example.mediaplayer.model.data.entities.Song
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
class MusicSource() {

    var songMeta = listOf<MediaMetadataCompat>()

    suspend fun fetchSongMeta(list: List<Song>) = withContext(Dispatchers.IO) {
        songMeta = list.map { song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST, song.artist)                   //1
                .putString(METADATA_KEY_ALBUM, song.album)                     //2
                .putString(METADATA_KEY_ALBUM_ART, song.imageUri)              //3
                .putString(METADATA_KEY_ALBUM_ARTIST, song.artist)             //4
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)             //5
                .putString(METADATA_KEY_MEDIA_ID, song.mediaId.toString())     //6
                .putString(METADATA_KEY_MEDIA_URI, song.mediaPath)             //7
                .putString(METADATA_KEY_TITLE, song.title)                     //8
                .build()
        }
    }

    // passable Concat for exoPlayer instead of direct MediaSource
    fun asConcat(dataSourceFactory: DefaultDataSource.Factory): ConcatenatingMediaSource {
        val concat = ConcatenatingMediaSource()
        songMeta.forEach { song ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(song.getString(METADATA_KEY_MEDIA_URI).toUri()))
        }
    }
}*/
