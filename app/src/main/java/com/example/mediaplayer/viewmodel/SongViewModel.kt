package com.example.mediaplayer.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.lifecycle.*
import com.example.mediaplayer.exoplayer.MusicServiceConnector
import com.example.mediaplayer.exoplayer.isPlayEnabled
import com.example.mediaplayer.exoplayer.isPlaying
import com.example.mediaplayer.exoplayer.isPrepared
import com.example.mediaplayer.model.data.entities.Album
import com.example.mediaplayer.model.data.entities.Artist
import com.example.mediaplayer.model.data.entities.Folder
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.model.data.local.MusicRepo
import com.example.mediaplayer.util.Constants.MEDIA_ROOT_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
            _mediaItems.postValue(items.toMutableList())
        }
    }

    init {
        Timber.d("SongViewModel init")
        updateMusicDB()
        musicServiceConnector.subscribe(MEDIA_ROOT_ID, subsCallback)
    }

    fun sendCommand(command: String, param: Bundle?, callback: (() -> Unit)?) {
        musicServiceConnector.sendCommand(command, param, callback)
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
            val list = musicDB.getAllSongs().toMutableList().also { songs ->
                _songList.value = songs.toMutableList()
                _albumList.value = songs.groupBy { it.album }.entries.map { (album, song) ->
                    Album(album, song)
                }
                _artistList.value = songs.groupBy { it.artist }.entries.map { (artist, song) ->
                    Artist(artist, song)
                }
                _folderList.value = musicDB.folderList.distinct().toMutableList()
            }

        }
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

    private suspend fun getShuffledSong(take: Int = _songList.value?.size ?: 0) : List<Song> {
        return _songList.value?.shuffled()?.take(take)
            ?: musicDB.getAllSongs()
    }

    fun checkShuffle() {
        // only check Shuffle after query, if somehow null then its empty
        viewModelScope.launch {
            val song = _songList.value ?: run {
                clearShuffle("songList is null")
                emptyList<Song>()
            }
            val shuf = _shuffles.value ?: run {
                Timber.d("shuffles : ${_shuffles.value}")
                getShuffledSong(song.size)
            }
            Timber.d(("shuffled : $shuf"))
            if (shuf.size > song.size) {
                withContext(Dispatchers.Main) {
                    clearShuffle("shuffle size is bigger than song list")
                }
            }
            val filtered = shuf.minus(song as List<Song>)
            withContext(Dispatchers.Main) {
                _shuffles.value = shuf.minus(filtered)
            }
        }
    }

    fun clearShuffle(msg: String) {
        Timber.d(msg)
        _shuffles.value = emptyList()
    }

    fun setCurFolder(folder: Folder) {
        _curFolder.value = folder
    }

    override fun onCleared() {
        super.onCleared()
    }
}