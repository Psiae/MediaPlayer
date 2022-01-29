package com.example.mediaplayer.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
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

class MusicSource() {

    /* this class simply will convert a List of Song fetched from MediaStore */

    var songMeta = listOf<MediaMetadataCompat>()

    /* rebuild fetched song with MediaMetadataCompat */
    suspend fun fetchSongMeta(list: List<Song>) = withContext(Dispatchers.IO) {
        songMeta = list.map { song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST, song.artist)                   //1
                .putString(METADATA_KEY_ALBUM, song.album)                     //2
                .putString(METADATA_KEY_ALBUM_ART, song.imageUri)              //3
                .putString(METADATA_KEY_ALBUM_ARTIST, song.artist)             //4
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)             //5
                .putLong(METADATA_KEY_DURATION, song.length)                   //6
                .putString(METADATA_KEY_DISPLAY_ICON_URI, song.imageUri)
                .putString(METADATA_KEY_MEDIA_ID, song.mediaId.toString())     //8
                .putString(METADATA_KEY_MEDIA_URI, song.mediaPath)             //9
                .putString(METADATA_KEY_TITLE, song.title)                     //10
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, "${song.dateModified}")
                .build()
        }
    }

    /** passable Concat for exoPlayer instead of direct MediaSource
     * use ProgressiveMediaSource for better Media handling
     * https://exoplayer.dev/progressive.html#using-progressivemediasource
     */

    fun asConcat(dataSourceFactory: DefaultDataSource.Factory): ConcatenatingMediaSource {
        val concat = ConcatenatingMediaSource()
        songMeta.forEach { song ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(song.getString(METADATA_KEY_MEDIA_URI).toUri()))
        }
        return concat
    }

    /* Add DescriptionCompat to the re-build song for display purposes on MediaBrowserCompat */
    fun asMediaItem() = songMeta.map { song ->
        val description = MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(description, FLAG_PLAYABLE and FLAG_BROWSABLE)
    }
}
