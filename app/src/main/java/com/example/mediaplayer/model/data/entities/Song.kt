package com.example.mediaplayer.model.data.entities

data class Song(

    var album: String = "",
    val albumId: Long = 0,
    var artist: String = "",
    val dateAdded: Int = 0,
    val dateModified: Int = 0,
    val displayName: String = "",
    val imageUri: String = "",
    val isLocal: Boolean = true,
    val length: Long = 0L,
    val mediaId: String = "",
    val mediaPath: String = "",
    val startFrom: Int = 0,
    var title: String = "",
    val year: Int = 0,

    // ID, ALBUM, ALBUM_ID, ARTIST, DATE_ADDED, DATE_MODIFIED, DISPLAY_NAME, DURATION, path, TITLE, YEAR
)
