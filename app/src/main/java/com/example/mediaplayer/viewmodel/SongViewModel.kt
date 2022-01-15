package com.example.mediaplayer.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.VersionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // there's no leak, stupid Inspector :)
class SongViewModel @Inject constructor(
    private val context: Context
) : ViewModel() {

    private val _songList = MutableLiveData<MutableList<Song>>()
    val songList: MutableLiveData<MutableList<Song>>
        get() = _songList

    fun postSongList(list: MutableList<Song>) {
        _songList.postValue(list)
    }

    fun getDeviceSong() {
        CoroutineScope(Dispatchers.IO).launch {
            val music = queryDeviceMusic()
            withContext(Dispatchers.Main) {
                _songList.value = music
            }
        }
    }
    private fun queryDeviceMusic(): MutableList<Song> {
        Timber.d(Thread.currentThread().toString())
        val deviceMusicList = mutableListOf<Song>()
        try {
            val mediaPath =
                if (VersionHelper.isQ()) MediaStore.Audio.AudioColumns.BUCKET_DISPLAY_NAME
                else MediaStore.Audio.AudioColumns.DATA

            val projection = arrayOf(
                MediaStore.Audio.AudioColumns._ID, //1
                MediaStore.Audio.AudioColumns.ALBUM, //2
                MediaStore.Audio.AudioColumns.ALBUM_ID, //3
                MediaStore.Audio.AudioColumns.ARTIST, //4
                MediaStore.Audio.AudioColumns.DATE_ADDED, //5
                MediaStore.Audio.AudioColumns.DATE_MODIFIED, //6
                MediaStore.Audio.AudioColumns.DISPLAY_NAME, //7
                MediaStore.Audio.AudioColumns.DURATION, //8
                mediaPath, //9
                MediaStore.Audio.AudioColumns.TITLE, // 10
                MediaStore.Audio.AudioColumns.YEAR, // 11
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1"
            val selectOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER
            val musicCursor = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
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
                val pathIndex =
                    cursor.getColumnIndexOrThrow(mediaPath)
                val titleIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
                val yearIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.YEAR)

                while (cursor.moveToNext()) {
                    Timber.d(cursor.toString())
                    val songId = cursor.getString(idIndex)
                    val album = cursor.getString(albumIndex)
                    val albumId = cursor.getLong(albumIdIndex)
                    val artist = cursor.getString(artistIndex)
                    val dateAdded = cursor.getString(dateAddedIndex)
                    val dateModified = cursor.getString(dateModifiedIndex)
                    val displayName = cursor.getString(displayNameIndex)
                    val duration = cursor.getString(durationIndex)
                    val path = cursor.getString(pathIndex)
                    val title = cursor.getString(titleIndex)
                    val year = cursor.getInt(yearIndex)

                    /*val artUri = Uri.parse("content://media/external/audio/albumart")
                    val albumArtUri = ContentUris.withAppendedId(artUri, albumId)*/

                    val audioPath =
                        if (VersionHelper.isQ()) path ?: "/" else {
                            val filePath = File(path).parentFile?.name ?: "/"
                            if (filePath != "0") filePath else "/"
                        }

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
                        year
                    ))

                    Timber.d("Added : ${deviceMusicList.last()}")
                }
            }
            Timber.d("$mediaPath \n $projection \n $selection \n $selectOrder \n $musicCursor")

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return deviceMusicList
    }
}