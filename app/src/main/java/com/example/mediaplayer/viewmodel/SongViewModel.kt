package com.example.mediaplayer.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER
import android.support.v4.media.session.MediaSessionCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
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
import com.example.mediaplayer.util.Resource
import com.example.mediaplayer.util.ext.toast
import com.example.mediaplayer.util.toMediaMetadataCompat
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
                val q = musicSource.getQueue().ifEmpty { musicSource.asSong() }
                q.find { it.mediaId.toString() == mediaItem?.description?.mediaId && it.queue == mediaItem.description?.description.toString().toLong() }
            }
        }

    var observedPlaying = Song()
    var currentlyPlayingSongListObservedByMainActivity = mutableListOf<Song>()

    fun addItemToQueue(song: Song, index: Int? = null) {
        val item = song.toMediaMetadataCompat()
        MediaMetadataCompat
            .Builder(item)
            .putLong(METADATA_KEY_TRACK_NUMBER, (musicSource.songs.lastIndex +1).toLong())
            .build().also {
                musicSource.songs.add(it)
                _mediaItems.value = musicSource.asSong()
            }
    }

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
                val pos = playbackState.value?.currentPlaybackPosition ?: -1L
                if(curPlayerPosition.value != pos && MusicService.curSongDuration > -1) {
                    _curPlayerPosition.postValue(pos)
                    _curSongDuration.postValue(MusicService.curSongDuration)
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
            return Transformations.map(_mediaItems) {
                it.sortedBy { it.queue }.toMutableList()
            }
        }

    val mediaItemSong: LiveData<MutableList<Song>>
        get() = mediaItems

    private var subsCallback = object : MediaBrowserCompat.SubscriptionCallback() {

        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>,
        ) {
            Timber.d("subs onLoadChildren")
            super.onChildrenLoaded(parentId, children)
            _mediaItems.value = musicSource.asSong()
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

    fun sendCommand(
        command: String,
        param: Bundle?,
        extra: String,
        songs: List<Song>,
        force: Boolean = false,
        playToggle: Boolean? = false,
        callback: (() -> Unit)?
    ) {
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
                if (callback != null) callback() else Timber.d("sendCommand is Null")
                musicSource.mapToSongs(filtered)
                _mediaItems.value = filtered.toMutableList()
                musicServiceConnector.sendCommand(command, param, callback)
                delay(200)
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

    fun getFromDB(): List<Song> {
        return musicDB.songFromQuery
    }

    fun fromQuery(): MutableList<Song> {
        return musicDB.fromQuery.toMutableList()
    }

    var newValue = 0

    fun updateSongList(fromDB: Boolean = true) {
        if (fromDB) viewModelScope.launch(Dispatchers.IO) {
            val oldList = fromQuery()
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
                Timber.d("oldList  ${oldList} != newList ${newList}")
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

    fun playOrToggle(mediaItem: Song, toggle: Boolean = false, caller: String) {
        Timber.d("Play or Toggle: ${mediaItem.title} ${mediaItem.queue} ${observedPlaying.queue}, caller: $caller")
        if (!playToggleEnabled) return
        val isPrepared = playbackState.value?.isPrepared ?: false
        try {
            if (isPrepared
                && (mediaItem.mediaId == observedPlaying.mediaId)
                && (mediaItem.queue == observedPlaying.queue)
            ) {
                playbackState.value?.let {
                    when {
                        it.isPlaying -> if (toggle) musicServiceConnector.transportControls.pause()
                        it.isPlayEnabled -> musicServiceConnector.transportControls.play()
                        else -> Unit
                    }
                }
            } else {
                val extra = Bundle().also { it.putLong("queue", mediaItem.queue ?: -1) }
                playFromMediaId(mediaItem, extra = extra)
            }
        } catch (e: Exception) {
            Timber.d("PlayToggleFailed")
        }
    }

    fun playFromMediaId(mediaItem: Song, extra: Bundle) {
        Timber.d("playFromMediaId extra = ${extra.getLong("queue")}")
        musicServiceConnector.transportControls
            .playFromMediaId(mediaItem.mediaId.toString(), extra)
    }

    /** Query */

    private fun getShuffledSong(take: Int) : List<Song> {
        return getFromDB().shuffled().take(take)
    }

    fun checkShuffle(song: List<Song>, msg: String) {
        // only check Shuffle after query, if somehow null then its empty
        Timber.d("checkShuffle ${song.size}")
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