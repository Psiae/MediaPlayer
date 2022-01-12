package com.example.mediaplayer.model.data.entities

data class Song(
    val artist: String = "",
    val album: String = "",
    val albumId: Int = 0,
    val dateAdded: Int = 0,
    val imageUri: String = "",
    val isLocal: Boolean = true,
    val length: Double = 0.0,
    val mediaId: String = "",
    val mediaUri: String = "",
    val mediaPath: String = "",
    val startFrom: Double = 0.0,
    val title: String = "",
    val year: Int = 0,
)
