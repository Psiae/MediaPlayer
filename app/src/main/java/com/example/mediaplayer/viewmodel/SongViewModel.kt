package com.example.mediaplayer.viewmodel

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
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
import com.example.mediaplayer.util.VersionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // there's no leak, stupid Inspector :)
class SongViewModel @Inject constructor(
    private val context: Context,
    private val musicServiceConnector: MusicServiceConnector,
    private val musicDB: MusicRepo
) : ViewModel() {

    /** Service Connector */

    val isConnected = musicServiceConnector.isConnected
    val onError = musicServiceConnector.networkError
    val playingMediaItem = musicServiceConnector.curPlayingSong
    val playbackState = musicServiceConnector.playbackState

    /** LiveData */

    val navHeight = MutableLiveData<Int>()
    val isPlaying = MutableLiveData<Boolean>(false)
    val curPlaying = MutableLiveData<Song>()
    val currentlyPlaying: LiveData<Song>
        get() {
            return Transformations.map(playingMediaItem) { mediaItem ->
                songList.value?.let { songs ->
                    songs.find { it.mediaId == mediaItem?.getString(METADATA_KEY_MEDIA_ID)?.toLong() }
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

    fun updateMusicDB() {
        if (_isFetching.value!!) return
        _isFetching.value = true

        viewModelScope.launch(Dispatchers.IO) {
            musicDB.setMusicList(queryDeviceMusic())
            _isFetching.postValue(false)
            updateSongList()
        }
    }

    fun getFromDB(): MutableList<Song> {
        return musicDB.getAllSongs().toMutableList()
    }

    fun updateSongList(fromDB: Boolean = true) {
        if (fromDB) viewModelScope.launch {
            _songList.value = musicDB.getAllSongs().toMutableList()
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
            ?: queryDeviceMusic().shuffled().take(take)
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

    private suspend fun queryDeviceMusic(): MutableList<Song> {
        Timber.d(Thread.currentThread().toString())
        val deviceMusicList = mutableListOf<Song>()
        val folderList = mutableListOf<Folder>()
        try {
            val mediaPath =
                if (VersionHelper.isQ()) MediaStore.Audio.AudioColumns.BUCKET_DISPLAY_NAME
                else MediaStore.Audio.AudioColumns.DATA

            val mediaPathId =
                if (VersionHelper.isQ()) MediaStore.Audio.AudioColumns.BUCKET_ID
                else MediaStore.Audio.AudioColumns.DATA

            val projection = arrayOf(
                MediaStore.Audio.AudioColumns._ID,              //1
                MediaStore.Audio.AudioColumns.ALBUM,            //2
                MediaStore.Audio.AudioColumns.ALBUM_ID,         //3
                MediaStore.Audio.AudioColumns.ARTIST,           //4
                MediaStore.Audio.AudioColumns.DATE_ADDED,       //5
                MediaStore.Audio.AudioColumns.DATE_MODIFIED,    //6
                MediaStore.Audio.AudioColumns.DISPLAY_NAME,     //7
                MediaStore.Audio.AudioColumns.DURATION,         //8
                MediaStore.Audio.AudioColumns.TITLE,            //9
                MediaStore.Audio.AudioColumns.YEAR,             //10
                mediaPath,                                      //11
                mediaPathId                                     //12
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0" // or 1
            val selectOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER
            val musicCursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, selectOrder
            )

            musicCursor?.use { cursor ->
                Timber.d(cursor.toString())

                val idIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val albumIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)
                val albumIdIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID)
                val artistIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)
                val dateAddedIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_ADDED)
                val dateModifiedIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED)
                val displayNameIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
                val durationIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
                val titleIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
                val yearIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.YEAR)
                val pathIndex =
                    cursor.getColumnIndexOrThrow(mediaPath)
                val folderIndex =
                    cursor.getColumnIndexOrThrow(mediaPathId)

                while (cursor.moveToNext()) {
                    Timber.d(cursor.toString())
                    val songId = cursor.getLong(idIndex)
                    val album = cursor.getString(albumIndex)
                    val albumId = cursor.getLong(albumIdIndex)
                    val artist = cursor.getString(artistIndex)
                    val dateAdded = cursor.getString(dateAddedIndex)
                    val dateModified = cursor.getString(dateModifiedIndex)
                    val displayName = cursor.getString(displayNameIndex)
                    val duration = cursor.getLong(durationIndex)
                    val title = cursor.getString(titleIndex)
                    val year = cursor.getInt(yearIndex)
                    val path = cursor.getString(pathIndex)
                    val folder = cursor.getString(folderIndex)

                    val audioPath =
                        if (VersionHelper.isQ()) path ?: "/"
                        else {
                            val filePath = File(path).parentFile?.name ?: "/"
                            if (filePath != "0") filePath else "/"
                        }

                    val folderPath = if (VersionHelper.isQ()) folder else File(folder).parent
                    val imagePath = Uri.parse("content://media/external/audio/albumart")
                    val imageUri = ContentUris.withAppendedId(imagePath, albumId)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

                    if (duration != 0L) {
                        deviceMusicList.add(Song(
                            album = album,
                            albumId = albumId,
                            artist = artist,
                            dateAdded = dateAdded.toInt(),
                            dateModified = dateModified.toInt(),
                            displayName = displayName,
                            imageUri = imageUri.toString(),
                            isLocal = true,
                            length = duration,
                            mediaId = songId,
                            mediaPath = audioPath,
                            mediaUri = uri.toString(),
                            startFrom = 0,
                            title = title,
                            year = year,
                        ))
                    } else Timber.d("$title ignored, duration : $duration")

                    if (!folderList.contains(Folder(audioPath, 0, folderPath))) {
                        folderList.add(Folder(
                            title = audioPath,
                            size = 0,
                            path = folderPath
                        )
                        )
                    }

                    Timber.d("$folderList $audioPath $folder")
                    Timber.d("Added : ${deviceMusicList.last()} $path")
                }
            }
            Timber.d("$mediaPath \n $projection \n $selection \n $selectOrder \n $musicCursor")

        } catch (e: Exception) {
            e.printStackTrace()
        }

        val listOfAlbum = mutableListOf<String>()
        val listOfArtist = mutableListOf<String>()

        val albumSong = deviceMusicList.groupBy { it.album }.entries.map { (album, song) ->
            listOfAlbum.add(album)
            Album(album, song)
        }

        val artistSong = deviceMusicList.groupBy { it.artist }.entries.map { (artist, song) ->
            listOfArtist.add(artist)
            Artist(artist, song)
        }


        Timber.d("artist: $listOfArtist \n album: $listOfAlbum")

        withContext(Dispatchers.Main) {
            _folderList.value = folderList
            _albumList.value = albumSong
            _artistList.value = artistSong
        }
        return deviceMusicList
    }

    override fun onCleared() {
        super.onCleared()
    }
}