package com.example.mediaplayer.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.media.session.PlaybackState
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.*
import com.example.mediaplayer.exoplayer.*
import com.example.mediaplayer.model.data.entities.Album
import com.example.mediaplayer.model.data.entities.Artist
import com.example.mediaplayer.model.data.entities.Folder
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.model.data.local.MusicRepo
import com.example.mediaplayer.util.Constants.FILTER_MODE_NONE
import com.example.mediaplayer.util.Constants.MEDIA_ROOT_ID
import com.example.mediaplayer.util.Constants.NOTIFY_CHILDREN
import com.example.mediaplayer.util.Constants.UPDATE_INTERVAL
import com.example.mediaplayer.util.Constants.UPDATE_SONG
import com.example.mediaplayer.util.Resource
import com.example.mediaplayer.util.ext.toast
import com.google.android.exoplayer2.Player
import com.google.common.base.Stopwatch
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
    val musicSource: MusicSource
) : ViewModel() {

    /** Service Connector */

    val isConnected = musicServiceConnector.isConnected
    val onError = musicServiceConnector.networkError
    val playingMediaItem = musicServiceConnector.curPlayingSong
    val playbackState = musicServiceConnector.playbackState
    val repeatState = musicServiceConnector.repeatMode

    /** LiveData */

    val navHeight = MutableLiveData<Int>()
    val curPlaying = MutableLiveData<Song>()
    val currentlyPlaying: LiveData<Song>
        get() {
            return Transformations.map(playingMediaItem) { mediaItem ->
                Timber.d("playingMediaItem : ${mediaItem?.description?.title}")
                getFromDB().find { it.mediaId == mediaItem?.getString(METADATA_KEY_MEDIA_ID)?.toLong() }
            }
        }

    var currentlyPlayingSongListObservedByMainActivity = mutableListOf<Song>()

    fun setRepeatMode(state: Int) {
        musicServiceConnector.transportControls.setRepeatMode(state)
    }

    fun checkQueue(): MutableList<MediaSessionCompat.QueueItem> {
        return musicServiceConnector.mediaController.queue
    }

    fun mediaBrowserConnected(): Boolean {
        return musicServiceConnector.checkMediaBrowser()
    }

    private val _curSongDuration = MutableLiveData<Long>()
    val curSongDuration: LiveData<Long> = _curSongDuration

    private val _curPlayerPosition = MutableLiveData<Long>()
    val curPlayerPosition: LiveData<Long> = _curPlayerPosition

    private fun updateCurrentPlayerPosition() {
        viewModelScope.launch(Dispatchers.IO) {
            while(true) {
                Timber.d("updateCurrentPlayerPos")
                val pos = playbackState.value?.currentPlaybackPosition ?: -1L
                if(curPlayerPosition.value != pos && MusicService.curSongDuration > -1) {
                    _curPlayerPosition.postValue(pos)
                    _curSongDuration.postValue(MusicService.curSongDuration)
                    Timber.d("playerPosUpdated")
                }
                delay(UPDATE_INTERVAL)
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

    val filterMode = MutableLiveData(FILTER_MODE_NONE)

    init {
        Timber.d("SongViewModel init")
        updateMusicDB()
        musicServiceConnector.subscribe(MEDIA_ROOT_ID, subsCallback)
        updateCurrentPlayerPosition()
    }

    var lastFilter = ""

    fun sendCommand(command: String, param: Bundle?, callback: (() -> Unit)?, extra: String, songs: List<Song>, force: Boolean = false, playToggle: Song? = null) {
        if (!stopwatch.isRunning || force) {
            if (!force) stopwatch.start()
            Timber.d("sendCommand")
            viewModelScope.launch {
                val songList = songs.ifEmpty { getFromDB() }.also {
                    updateMusicDB()
                }
                if (songList.isNullOrEmpty()) {
                    Timber.d("songList isNullOrEmpty")
                    updateMusicDB()
                    return@launch
                }
                val filtered = if (extra.isNotEmpty()) {
                    lastFilter = extra
                    songList.filter { it.artist == extra || it.album == extra || it.title == extra }
                } else {
                    lastFilter = ""
                    songList
                }

                musicSource.mapToSongs(filtered)
                musicServiceConnector.sendCommand(command, param, callback)
                delay(300)
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

    var newValue = 0

    fun updateSongList(fromDB: Boolean = true) {
        if (fromDB) viewModelScope.launch(Dispatchers.IO) {
            val oldList = getFromDB()
            val source = musicSource.songs
            val newList = musicDB.getAllSongs().toMutableList().also { songs ->
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
            if (oldList != newList) {
                Timber.d("oldList != newList")
                val lastPlayingIndex = MusicService.lastItemIndex
                val lastPlayingMediaId = MusicService.curSongMediaId
                val lastPlayingPosition = curPlayerPosition.value
                val isPlaying = playbackState.value?.isPlaying
                val item = newList.find { it.mediaId == lastPlayingMediaId }
                if (musicServiceConnector.isControllerInit()) {
                    if (oldList.size != source.size) {
                        sendCommand(NOTIFY_CHILDREN, null, null, lastFilter, newList, true)
                    } else sendCommand(NOTIFY_CHILDREN, null, null, "", newList, true)

                    viewModelScope.launch {
                        delay(200)
                        try {
                            val item = newList.find { it.mediaId == lastPlayingMediaId }
                            item?.let {
                                playFromMediaId(item)
                                Timber.d(" isPlaying $isPlaying")
                                delay(700)
                                if (curPlaying.value == item) {
                                    seekTo(lastPlayingPosition ?: 0)

                                    if (isPlaying == false && curPlaying.value == item) {
                                        Timber.d("paused Music")
                                        musicServiceConnector.transportControls.pause()
                                    } else musicServiceConnector.transportControls.play()
                                }

                            } ?: run {
                                if (lastPlayingIndex < newList.size) {
                                    val item = newList[lastPlayingIndex]
                                    playFromMediaId(item)
                                    delay(500)
                                    if (isPlaying == false && curPlaying.value == item)
                                        musicServiceConnector.transportControls.pause()
                                    else musicServiceConnector.transportControls.play()
                                }
                            }
                        } catch (e : Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun List<MediaMetadataCompat>.toSong(): List<Song> {
        val mediaItemList = mutableListOf<Song>()
        val dbList = getFromDB()
        this.map { item ->
            mediaItemList.add(dbList.find { it.mediaId.toString() == item.getString(METADATA_KEY_MEDIA_ID)} ?: Song())
        }
        val filtered = mediaItemList.filter { it.mediaId != 0L }.toMutableList()
        return filtered.ifEmpty { emptyList<Song>().toMutableList() }
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
        Timber.d("Skip Prev to")
        musicServiceConnector.transportControls.skipToPrevious()
    }

    var seekToEnabled = true

    fun pause() = musicServiceConnector.transportControls.pause()
    fun play() = musicServiceConnector.transportControls.play()

    fun seekTo(pos: Long){
        if (!seekToEnabled) return
        Timber.d("Seek To $pos Long")
        musicServiceConnector.transportControls.seekTo(pos)
    }

    fun seekTo(pos: Double) {
        if (!seekToEnabled) return
        Timber.d("Seek To $pos Percent")
        musicServiceConnector.transportControls.seekTo((MusicService.curSongDuration * pos).toLong())
    }

    var playToggleEnabled = true

    fun playOrToggle(mediaItem: Song, toggle: Boolean = false, callback: (() -> Unit)? = null) {
        Timber.d("Play or Toggle: ${mediaItem.title}")
        if (!playToggleEnabled) return
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
                playFromMediaId(mediaItem)
            }
        } catch (e: Exception) {
            Timber.d("PlayToggleFailed")
        }
    }

    fun playFromMediaId(mediaItem: Song) {
        musicServiceConnector.transportControls
            .playFromMediaId(mediaItem.mediaId.toString(), null)
        Timber.d("playFromMediaId ${mediaItem.title}")
    }

    /** Query */

    private fun getShuffledSong(take: Int) : List<Song> {
        return getFromDB().shuffled().take(take)
    }

    fun checkShuffle(song: List<Song>, msg: String) {
        // only check Shuffle after query, if somehow null then its empty
        Timber.d("checkShuffle $song")
        val size = if (song.size < 20) song.size else 20
        viewModelScope.launch {
            if (song.isNullOrEmpty()) {
                getFromDB()
            } else {
                var shuf = _shuffles.value ?: run {
                    getShuffledSong(size)
                }
                if (shuf.isEmpty() && song.isNotEmpty()) {
                    Timber.d("shuffle is empty but songList is not")
                    shuf = getShuffledSong(size)
                }
                Timber.d(("shuffled : $shuf"))
                if (shuf.size > song.size) {
                    if (shuf.size >= getFromDB().size) {
                        getShuffledSong(size).also {
                            if (it.isEmpty()) clearShuffle("Empty Size")
                        }
                    }
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