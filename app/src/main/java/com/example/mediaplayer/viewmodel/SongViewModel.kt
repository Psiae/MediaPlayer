package com.example.mediaplayer.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mediaplayer.model.data.entities.Folder
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.VersionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // there's no leak, stupid Inspector :)
class SongViewModel @Inject constructor(
    private val context: Context
) : ViewModel() {

    val navHeight = MutableLiveData<Int>()
    val curPlayingSong = MutableLiveData<Song>()
    val isPlaying = MutableLiveData<Boolean>(false)

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

    fun setCurFolder(folder: Folder) {
        _curFolder.value = folder
    }

    suspend fun getDeviceSong(msg: String = "unknown") {
        delay(100)
        if (_isFetching.value!!) return
        withContext(Dispatchers.Main) {
            _isFetching.value = true
        }
        CoroutineScope(Dispatchers.IO).launch {
            Timber.d("getDeviceSong by $msg")
            queryDeviceMusic()
            _isFetching.postValue(false)
        }
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
                    val duration = cursor.getString(durationIndex)
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

                    deviceMusicList.add(Song(
                        album,
                        albumId,
                        artist,
                        dateAdded.toInt(),
                        dateModified.toInt(),
                        displayName,
                        imageUri = "",
                        isLocal = true,
                        duration.toLong(),
                        songId,
                        audioPath,
                        startFrom = 0,
                        title,
                        year,
                    ))

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
        withContext(Dispatchers.Main.immediate) {
            _songList.value = deviceMusicList
            _folderList.value = folderList
        }
        return deviceMusicList
    }
}