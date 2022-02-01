package com.example.mediaplayer.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.lifecycle.*
import com.example.mediaplayer.exoplayer.*
import com.example.mediaplayer.model.data.entities.Album
import com.example.mediaplayer.model.data.entities.Artist
import com.example.mediaplayer.model.data.entities.Folder
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.model.data.local.MusicRepo
import com.example.mediaplayer.util.Constants.MEDIA_ROOT_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
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
    val mediaItemPlaying: LiveData<MediaMetadataCompat?>
        get() = playingMediaItem
    val currentlyPlaying: LiveData<Song>
        get() {
            return Transformations.map(playingMediaItem) { mediaItem ->
                Timber.d("playingMediaItem : ${mediaItem!!.description.title}")
                songList.value?.let { songs ->
                    songs.find {
                        it.mediaId == mediaItem?.getString(METADATA_KEY_MEDIA_ID)?.toLong()
                    }
                } ?: run {
                    _songList.value = getFromDB()
                    songList.value!!.find { it.mediaId == mediaItem?.getString(METADATA_KEY_MEDIA_ID)?.toLong() }
                }
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
                mediaItems.map {  item ->
                    mediaItemList.add(songList.value?.find { it.mediaId == item.mediaId }!!)
                }.toMutableList()
                mediaItemList.ifEmpty { emptyList<Song>().toMutableList() }
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
            _mediaItems.value = items.toMutableList()
        }
    }

    init {
        Timber.d("SongViewModel init")
        updateMusicDB()
        musicServiceConnector.subscribe(MEDIA_ROOT_ID, subsCallback)
    }

    fun sendCommand(command: String, param: Bundle?, callback: (() -> Unit)?, extra: String) {
        viewModelScope.launch() {
            val songList = songList.value ?: emptyList<Song>()
            if (songList.isNullOrEmpty()) {
                Timber.d("songList isNullOrEmpty")
                updateSongList()
                return@launch
            }
            if (extra.isNotEmpty()) musicSource.mapToSongs(songList.filter { it.artist == extra || it.album == extra })
            else musicSource.mapToSongs(songList)
            musicServiceConnector.sendCommand(command, param, callback)
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
        var list = emptyList<Song>()
        viewModelScope.launch { list = musicDB.getAllSongs() }
        return list.toMutableList()
    }

    fun updateSongList(fromDB: Boolean = true) {
        if (fromDB) viewModelScope.launch {
            musicDB.getAllSongs().toMutableList().also { songs ->
                _albumList.value = songs.groupBy { it.album }.entries.map { (album, song) ->
                    Album(album, song)
                }
                _artistList.value = songs.groupBy { it.artist }.entries.map { (artist, song) ->
                    Artist(artist, song)
                }
                _folderList.value = musicDB.folderList.distinct().toMutableList()
                _songList.value = songs.toMutableList()
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
            }
        } catch (e: Exception) {
            Timber.d("PlayToggleFailed")
        }
    }

    /** Query */

    private suspend fun getShuffledSong(take: Int = songList.value?.size ?: 0) : List<Song> {
        return songList.value?.shuffled()?.take(take)
            ?: musicDB.getAllSongs()
    }

    fun checkShuffle() {
        // only check Shuffle after query, if somehow null then its empty
        viewModelScope.launch {
            val song = songList.value ?: run {
                clearShuffle("songList is null")
                emptyList<Song>()
            }
            var shuf = shuffles.value ?: run {
                Timber.d("shuffles : ${shuffles.value}")
                getShuffledSong(song.size)
            }
            if (shuf.isEmpty() && song.isNotEmpty()) {
                Timber.d("shuffle is empty but songList is not")
                 shuf = getShuffledSong(song.size)
            }
            Timber.d(("shuffled : $shuf"))
            if (shuf.size > song.size) {
                withContext(Dispatchers.IO) {
                    if (shuf.size >= getFromDB().size) clearShuffle("shuffle size is bigger but covered by db")
                    else Timber.d("shuffle size is bigger than songList")
                }
            }

            val filtered = shuf.minus(song as List<Song>)
            withContext(Dispatchers.Main) {
                _shuffles.value = shuf.minus(filtered)
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