package com.example.mediaplayer.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.widget.Toast
import androidx.lifecycle.*
import com.example.mediaplayer.exoplayer.*
import com.example.mediaplayer.model.data.entities.Album
import com.example.mediaplayer.model.data.entities.Artist
import com.example.mediaplayer.model.data.entities.Folder
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.model.data.local.MusicRepo
import com.example.mediaplayer.util.Constants
import com.example.mediaplayer.util.Constants.MEDIA_ROOT_ID
import com.example.mediaplayer.util.Resource
import com.example.mediaplayer.util.ext.toast
import com.google.common.base.Stopwatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // there's no leak, stupid Inspector :)
class SongViewModel @Inject constructor(
    private val context: Context,
    private val musicServiceConnector: MusicServiceConnector,
    private val musicDB: MusicRepo,
    private val musicSource: MusicSource
) : ViewModel() {

    /** Service Connector */

    val isConnected = musicServiceConnector.isConnected
    val onError = musicServiceConnector.networkError
    val playingMediaItem = musicServiceConnector.curPlayingSong
    val playbackState = musicServiceConnector.playbackState

    /** LiveData */

    val navHeight = MutableLiveData<Int>()
    val curPlaying = MutableLiveData<Song>()
    val currentlyPlaying: LiveData<Song>
        get() {
            return Transformations.map(playingMediaItem) { mediaItem ->
                Timber.d("playingMediaItem : ${mediaItem!!.description.title}")
                getFromDB().find { it.mediaId == mediaItem?.getString(METADATA_KEY_MEDIA_ID)?.toLong() }/*
                songList.value?.let { songs ->
                    songs.find {
                        it.mediaId == mediaItem?.getString(METADATA_KEY_MEDIA_ID)?.toLong()
                    }
                } ?: run {
                    _songList.value = getFromDB().also {
                        Timber.d("setSongList ${it.size}")
                    }
                    songList.value!!.find { it.mediaId == mediaItem?.getString(METADATA_KEY_MEDIA_ID)?.toLong() }
                }*/
            }
        }

    var currentlyPlayingSongListObservedByMainActivity = mutableListOf<Song>()

    fun mediaBrowserConnected(): Boolean {
        return musicServiceConnector.checkMediaBrowser()
    }

    private val _shuffles = MutableLiveData<List<Song>>()
    val shuffles: LiveData<List<Song>>
        get() {
            Timber.d("curShuffle liveData")
            return _shuffles
        }

    private val _artistList = MutableLiveData<List<Artist>>()
    val artistList: LiveData<List<Artist>>
        get() {
            Timber.d("artistList liveData")
            return _artistList
        }

    private val _curArtist = MutableLiveData<Artist>()
    val curArtist: LiveData<Artist>
        get() {
            Timber.d("curArtist liveData")
            return _curArtist
        }

    private val _albumList = MutableLiveData<List<Album>>()
    val albumList: LiveData<List<Album>>
        get() {
            Timber.d("listAlbum liveData")
            return _albumList
        }

    private val _curAlbum = MutableLiveData<Album>()
    val curAlbum: LiveData<Album>
        get() {
            Timber.d("curArtist liveData")
            return _curAlbum
        }

    private val _curFolder = MutableLiveData<Folder>()
    val curFolder: LiveData<Folder>
        get() {
            Timber.d("curFolder liveData")
            return _curFolder
        }

    private val _songList = MutableLiveData<MutableList<Song>>()
    val songList: LiveData<MutableList<Song>>
        get() {
            Timber.d("songList liveData")
            return _songList
        }

    private val _folderList = MutableLiveData<MutableList<Folder>>()
    val folderList: LiveData<MutableList<Folder>>
        get() {
            Timber.d("folderList liveData")
            return _folderList
        }

    private val _isFetching = MutableLiveData(false)

    private val _resMediaItems = MutableLiveData<Resource<List<Song>>>()
    val resMediaItems: LiveData<Resource<List<Song>>> = _resMediaItems

    private val _mediaItems = MutableLiveData<MutableList<Song>>()
    val mediaItems: LiveData<MutableList<Song>>
        get() {
            Timber.d("mediaItems LiveData")
            return _mediaItems
        }

    val mediaItemSong: LiveData<MutableList<Song>>
        get() {
            return Transformations.map(mediaItems) { mediaItems ->
                val mediaItemList = mutableListOf<Song>()
                val dbList = getFromDB()
                mediaItems.map {  item ->
                    mediaItemList.add(dbList.find { it.mediaId == item.mediaId } ?: Song())
                }
                val filtered = mediaItemList.filter { it.mediaId != 0L }.toMutableList()
                filtered.ifEmpty { emptyList<Song>().toMutableList() }
            }
        }

    private var subsCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>,
        ) {
            Timber.d("subs onLoadChildren")
            super.onChildrenLoaded(parentId, children)
            val items = children.map {
                Song(
                    mediaId = it.mediaId!!.toLong(),
                    title = it.description.title.toString(),
                    mediaPath = it.description.mediaUri.toString(),
                    imageUri = it.description.iconUri.toString()
                )
            }
            _resMediaItems.value = Resource.success(items)
            _mediaItems.value = items.toMutableList()
        }
    }
    private val stopwatch = Stopwatch.createUnstarted()

    init {
        Timber.d("SongViewModel init")
        updateMusicDB()
        musicServiceConnector.subscribe(MEDIA_ROOT_ID, subsCallback)
    }

    fun sendCommand(command: String, param: Bundle?, callback: (() -> Unit)?, extra: String, songs: List<Song>) {
        if (!stopwatch.isRunning) {
            stopwatch.start()
            Timber.d("sendCommand")
            viewModelScope.launch() {
                val songList = songs.ifEmpty { getFromDB() }
                if (songList.isNullOrEmpty()) {
                    Timber.d("songList isNullOrEmpty")
                    updateSongList()
                    return@launch
                }
                val filtered =
                    if (extra.isNotEmpty()) songList.filter { it.artist == extra || it.album == extra }
                    else songList
                musicSource.mapToSongs(filtered)
                musicServiceConnector.sendCommand(command, param, callback)
                delay(500)
                stopwatch.reset()
            }
        } else {
            viewModelScope.launch { toast(context, "Slow Down") }
        }
    }

    fun updateMusicDB() {
        if (_isFetching.value!!) return
        _isFetching.value = true

        viewModelScope.launch(Dispatchers.IO) {
            updateSongList()
            _isFetching.postValue(false)
        }
    }

    fun getFromDB(): MutableList<Song> {
        return musicDB.songFromQuery
    }

    fun updateSongList(fromDB: Boolean = true) {
        if (fromDB) viewModelScope.launch(Dispatchers.IO) {
            musicDB.getAllSongs().toMutableList().also { songs ->
                withContext(Dispatchers.Main) {
                    _albumList.value = songs.groupBy { it.album }.entries.map { (album, song) ->
                        Album(album, song)
                    }
                    _artistList.value = songs.groupBy { it.artist }.entries.map { (artist, song) ->
                        Artist(artist, song)
                    }
                    _folderList.value = musicDB.folderList.distinct().toMutableList()
                    _songList.value = songs.toMutableList().also {
                        Timber.d("setSongList ${it.size}")
                    }
                }
            }
        }
    }

    fun findMatchingMediaId(mediaItem: MediaMetadataCompat): Song {
        return songList.value!!.find { it.mediaId == mediaItem.getLong(
            METADATA_KEY_MEDIA_ID
        ) } ?: songList.value!![0]
    }

    fun skipNext() {
        Timber.d("Skip Next")
        musicServiceConnector.transportControls.skipToNext()
    }

    fun skipPrev() {
        Timber.d("Skip Prev")
        musicServiceConnector.transportControls.skipToPrevious()
    }

    fun seekTo(pos: Long){
        Timber.d("Seek To")
        musicServiceConnector.transportControls.seekTo(pos)
    }

    fun playOrToggle(mediaItem: Song, toggle: Boolean = false) {
        Timber.d("Play or Toggle: ${mediaItem.title}")
        val isPrepared = playbackState.value?.isPrepared ?: false
        try {
            if (isPrepared && mediaItem.mediaId ==
                playingMediaItem.value?.getString(METADATA_KEY_MEDIA_ID)?.toLong()
            ) {
                playbackState.value?.let {
                    when {
                        it.isPlaying -> if (toggle) musicServiceConnector.transportControls.pause()
                        it.isPlayEnabled -> musicServiceConnector.transportControls.play()
                        else -> Unit
                    }
                }
            } else {
                musicServiceConnector.transportControls
                    .playFromMediaId(mediaItem.mediaId.toString(), null)
                curPlaying.value = mediaItem
                Timber.d("playFromMediaId ${mediaItem.title}")
            }
        } catch (e: Exception) {
            Timber.d("PlayToggleFailed")
        }
    }

    /** Query */

    private fun getShuffledSong(take: Int) : List<Song> {
        return getFromDB().shuffled().take(take)
    }

    fun checkShuffle(song: List<Song>, msg: String) {
        // only check Shuffle after query, if somehow null then its empty
        Timber.d("checkShuffle $song")
        viewModelScope.launch {
            if (song.isNullOrEmpty()) {
                getFromDB()
            } else {
                var shuf = _shuffles.value ?: run {
                    getShuffledSong(song.size)
                }
                if (shuf.isEmpty() && song.isNotEmpty()) {
                    Timber.d("shuffle is empty but songList is not")
                    shuf = getShuffledSong(song.size)
                }
                Timber.d(("shuffled : $shuf"))
                if (shuf.size > song.size) {
                    if (shuf.size >= getFromDB().size) clearShuffle("shuffle size is bigger but covered by db")
                    else Timber.d("shuffle size is bigger than songList")
                }
                val filtered = shuf.minus(song)
                withContext(Dispatchers.Main) {
                    _shuffles.value = shuf.minus(filtered)
                }
            }
        }
    }

    fun clearShuffle(msg: String) {
        Timber.d("clearShuffle: $msg")
        _shuffles.postValue(listOf(Song(title = "EMPTY")))
    }

    fun setCurFolder(folder: Folder) {
        _curFolder.value = folder
    }

    override fun onCleared() {
        super.onCleared()
    }
}