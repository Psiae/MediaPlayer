package com.example.mediaplayer.exoplayer

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import com.example.mediaplayer.model.data.entities.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicSource() {

    val songMeta = listOf<MediaMetadataCompat>()

    /*suspend fun fetchSongMeta(list: List<Song>) = withContext(Dispatchers.IO) {
        val songs = list
        songMeta = songs.map { song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                .putString(METADATA_KEY_ARTIST, song.artist)
                .putString(METADATA_KEY_ALBUM, song.album)
                .putString(METADATA_KEY_MEDIA_ID, song.mediaId)
                .putString(METADATA_KEY_ALBUM_ARTIST, song.artist)
                .putString(METADATA_KEY_ALBUM, song.album)
                .putString(METADATA_KEY_URI, song.mediaPath)
        }
    }*/
}