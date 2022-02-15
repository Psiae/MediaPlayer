package com.example.mediaplayer.exoplayer

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.example.mediaplayer.exoplayer.State.*
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.model.data.local.MusicRepo
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class MusicSource (
    val musicDatabase: MusicRepo,
    private val context: Context
) {

    var init = true

    var songs = listOf<MediaMetadataCompat>()
    var queuedSong = listOf<Song>()

    fun getFromDB(): List<Song> {
        return musicDatabase.songFromQuery
    }

    suspend fun mapToSongs(songToMap: List<Song>): List<Song> {
        var toReturn = listOf<Song>()
        withContext(Dispatchers.IO) {
            var i = 0L
            songs = songToMap.map { song ->
                Builder()
                    .putString(METADATA_KEY_ARTIST, song.artist)
                    .putString(METADATA_KEY_MEDIA_ID, song.mediaId.toString())
                    .putString(METADATA_KEY_TITLE, song.title)
                    .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                    .putString(METADATA_KEY_DISPLAY_ICON_URI, song.imageUri)
                    .putString(METADATA_KEY_MEDIA_URI, song.mediaUri)
                    .putString(METADATA_KEY_ALBUM_ART_URI, song.imageUri)
                    .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.artist)
                    .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.displayName)
                    .putLong(METADATA_KEY_TRACK_NUMBER, i++)
                    .build()
            }.toMutableList().also {
                val songs = asSong(it)
                queuedSong = songs
                toReturn = songs
            }
        }
        state = STATE_INITIALIZED
        return toReturn
    }

    suspend fun fetchMediaData() = withContext(Dispatchers.IO) {
        state = STATE_INITIALIZING
        val allSongs: List<Song> = musicDatabase.getAllSongs()
        Timber.d("AllSongs")
        mapToSongs(allSongs)
    }

    fun asSong(meta: List<MediaMetadataCompat>? = null): MutableList<Song> {
        val songList = getFromDB()
        val toReturn = mutableListOf<Song>()

        meta?.let { metas ->
            metas.forEach { meta ->
                songList.find {
                    it.mediaId.toString() == meta.description.mediaId
                }?.let {
                    val song = Song(
                        album = it.album, albumId = it.albumId, artist = it.artist,
                        dateAdded = it.dateAdded, dateModified = it.dateModified,
                        displayName = it.displayName, imageUri = it.imageUri, isLocal = it.isLocal,
                        length = it.length, mediaId = it.mediaId, mediaPath = it.mediaPath,
                        mediaUri = it.mediaUri, startFrom = 0, title = it.title, year = it.year,
                        queue = meta.getLong(METADATA_KEY_TRACK_NUMBER)
                    )
                    toReturn.add(song)
                }
            }
        } ?: songs.forEach { song ->
            songList.find { item ->
                item.mediaId.toString() == song.description.mediaId
            }?.let {
                val song = Song(
                    album = it.album, albumId = it.albumId, artist = it.artist,
                    dateAdded = it.dateAdded, dateModified = it.dateModified,
                    displayName = it.displayName, imageUri = it.imageUri, isLocal = it.isLocal,
                    length = it.length, mediaId = it.mediaId, mediaPath = it.mediaPath,
                    mediaUri = it.mediaUri, startFrom = 0, title = it.title, year = it.year,
                    queue = song.getLong(METADATA_KEY_TRACK_NUMBER)
                )
                toReturn.add(song)
            }
        }
        return toReturn
    }

    suspend fun indexSong(song: MutableList<Song>): MutableList<Song> {
        val toReturn = mutableListOf<Song>()
        withContext(Dispatchers.IO) {
            var i = 0L
            song.forEach {
                toReturn.add(Song(
                    album = it.album, albumId = it.albumId, artist = it.artist,
                    dateAdded = it.dateAdded, dateModified = it.dateModified,
                    displayName = it.displayName, imageUri = it.imageUri, isLocal = it.isLocal,
                    length = it.length, mediaId = it.mediaId, mediaPath = it.mediaPath,
                    mediaUri = it.mediaUri, startFrom = 0, title = it.title, year = it.year,
                    queue = i++
                ))
            }
        }
        return toReturn
    }

    fun asMediaSource(dataSourceFactory: DefaultDataSource.Factory): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach { song ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(
                    MediaItem.Builder()
                        .setUri(song.getString(METADATA_KEY_MEDIA_URI))
                        .setMediaId(song.description.mediaId!!)
                        .build()
                )
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    fun asMediaItems() = songs.map { song ->
        val desc = MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
    }.toMutableList()

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    private var state: State = STATE_CREATED
        set(value) {
            if(value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    fun whenReady(action: (Boolean) -> Unit): Boolean {
        return if(state == STATE_CREATED || state == STATE_INITIALIZING) {
            onReadyListeners += action
            false
        } else {
            action(state == STATE_INITIALIZED)
            true
        }
    }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}















