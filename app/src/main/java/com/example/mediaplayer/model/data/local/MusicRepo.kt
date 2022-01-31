package com.example.mediaplayer.model.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.mediaplayer.model.data.entities.Folder
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.VersionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class MusicRepo(private val context: Context) {

    suspend fun getAllSongs(): List<Song> {
        return try {
            return queryDeviceMusic()
        } catch(e: Exception) {
            emptyList()
        }
    }

    var folderList = mutableListOf<Folder>()

    private suspend fun queryDeviceMusic(): MutableList<Song> {
        Timber.d(Thread.currentThread().toString())
        val deviceMusicList = mutableListOf<Song>()
        val listOfFolder = mutableListOf<Folder>()
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

                    withContext(Dispatchers.Main) {

                    }

                    if (!listOfFolder.contains(Folder(audioPath, 0, folderPath))) {
                        listOfFolder.add(Folder(
                            title = audioPath,
                            size = 0,
                            path = folderPath
                        )
                        )
                    }
                }
            }
            Timber.d("$mediaPath \n $projection \n $selection \n $selectOrder \n $musicCursor")

        } catch (e: Exception) {
            e.printStackTrace()
        }
        folderList = listOfFolder
        return deviceMusicList
    }
}